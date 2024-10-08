(ns vemv
  "For one-off code or experiments"
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.spec.alpha :as spec]
   [formatting-stack.processors.test-runner]
   [clojure.test]
   [formatting-stack.linters.one-resource-per-ns]
   [clojure.string :as string]
   [clojure.core.async :as a]
   [nedap.speced.def :as speced]))

(defn pinto [parallelism coll xf xs]
  (let [dest (a/chan)]
    (a/pipeline parallelism dest xf (a/to-chan! xs))
    (a/<!! (a/into coll dest))))

(defn is=
  "Performs `(is = ...)`, reporting the diff between `expected` and `actual` when the test assertion fails."
  ([expected actual]
   (is= expected actual nil))

  ([expected actual desc]
   (clojure.test/testing (str (when (seq clojure.test/*testing-contexts*)
                                "\n") ;; place a newline so that prior strings and e.g. a large hashmap aren't rendered in the same line
                              (with-out-str
                                (-> expected
                                    ((requiring-resolve 'lambdaisland.deep-diff/diff) actual)
                                    ((requiring-resolve 'lambdaisland.deep-diff/pretty-print) ((requiring-resolve 'lambdaisland.deep-diff/printer) {:print-color false})))))
     (clojure.test/is (= expected actual)
                      desc))))


(speced/defn ns-sym->filename* [^simple-symbol? ns-sym]
  (some-> (list 'ns ns-sym)
          (formatting-stack.linters.one-resource-per-ns/ns-decl->resource-path ".clj")
          formatting-stack.linters.one-resource-per-ns/resource-path->filenames
          first
          (string/replace "file:" "")))

(def ns-sym->filename (memoize ns-sym->filename*))

(defn commit! [& directives]
  (and (formatting-stack.processors.test-runner/test!)
       #_ (some-> 'user.format/clean-staged-namespaces! resolve deref .call)
       (do
         (sh "git" "add" "-A")
         ;; XXX backup branch
         (sh "git" "commit")))
  (when (->> directives (some #{:p}))
    (sh "git" "push")))

(def fdef @#'spec/fdef) ;; can take value of a macro after all

(alter-var-root #'spec/fdef (constantly
                             (fn [form env fn-sym & specs]
                               (let [v (apply fdef form env fn-sym specs)
                                     s (symbol (str (name fn-sym)
                                                    "--fdef-source"))
                                     {:keys [line column]} (meta form)]
                                 (eval (list 'def
                                             s
                                             (list 'quote form)))
                                 (alter-meta! (ns-resolve *ns* s)
                                              assoc
                                              :line line
                                              :column column)
                                 v))))

(alter-meta! #'spec/fdef assoc :macro true)

(defn run-tests [& namespaces]
  (let [summary (-> (->> namespaces
                         (reduce (bound-fn [r n]
                                   (let [{:keys [fail error]
                                          :as v} (clojure.test/test-ns n)
                                         failed? (some pos? [fail error])
                                         ret (merge-with + r v)]
                                     (cond-> ret
                                       (or failed?
                                           (-> (Thread/currentThread) .isInterrupted)) reduced)))
                                 clojure.test/*initial-report-counters*))
                    (assoc :type :summary))]
    (clojure.test/do-report summary)
    summary))

(defn run-all-tests []
  (->> (all-ns)
       (filter (fn [n]
                 (->> n
                      ns-publics
                      vals
                      (some (fn [var-ref]
                              {:pre [(var? var-ref)]}
                              (-> var-ref meta :test))))))
       (sort-by pr-str)
       (reverse) ;; unit first
       (apply run-tests)))

(defmacro testing [string & body]
  `(binding [clojure.test/*testing-contexts* (conj clojure.test/*testing-contexts* (str (clojure.string/trim (str ~string)) "\n"))]
     ~@body))

(alter-var-root #'clojure.test/testing (constantly @#'testing))

(doseq [i '[hash-map list map not-empty set vector vector-distinct fmap elements
            bind choose fmap one-of such-that tuple sample return
            large-integer* double* frequency shuffle]
        :let [source (requiring-resolve (symbol "clojure.test.check.generators" (str i)))
              dest (requiring-resolve (symbol "clojure.spec.gen.alpha" (str i)))
              doc (-> source meta :doc)
              arglists (-> source meta :arglists)]]
  (alter-meta! dest merge {:arglists arglists
                           :doc doc}))
