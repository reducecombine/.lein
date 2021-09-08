(ns vemv
  "For one-off code or experiments"
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.spec.alpha :as spec]
   [formatting-stack.processors.test-runner]
   [clojure.test]
   [formatting-stack.linters.one-resource-per-ns]
   [lambdaisland.deep-diff]
   [clojure.string :as string]
   [nedap.speced.def :as speced]))

(defn is=
  "Performs `(is = ...)`, reporting the diff between `expected` and `actual` when the test assertion fails."
  ([expected actual]
   (is= expected actual nil))

  ([expected actual desc]
   (clojure.test/testing (str (when (seq clojure.test/*testing-contexts*)
                                "\n") ;; place a newline so that prior strings and e.g. a large hashmap aren't rendered in the same line
                              (with-out-str
                                (-> expected
                                    (lambdaisland.deep-diff/diff actual)
                                    (lambdaisland.deep-diff/pretty-print (lambdaisland.deep-diff/printer {:print-color false})))))
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
