(ns vemv.emacs-backend
  "Performs `require`s that my Emacs setup assumes."
  (:require
   [clojure.java.io :as io]
   [vemv.tap]
   [vemv.usages]
   [vemv]))

#_ (try
     ;; explicit require so that refactor-nrepl can discover it
     ;; NOTE: this must be placed before any refactor-nrepl require.
     (require 'clojure.tools.nrepl)
     (catch Exception _
       ;; most people don't use c.t.nrepl
       ))

(clojure.core/require 'clj-stacktrace.repl)
(clojure.core/require 'refactor-nrepl.core)
(clojure.core/require 'refactor-nrepl.middleware)
(clojure.core/require 'refactor-nrepl.analyzer)
(clojure.core/require 'refactor-nrepl.ns.class-search)
(clojure.core/require 'net.vemv.nrepl-debugger)
(clojure.core/require 'clj-java-decompiler.core)
(clojure.core/require 'criterium.core)
(clojure.core/require 'clojure.tools.namespace.repl)
(clojure.core/require 'com.stuartsierra.component.repl)

(require 'refactor-nrepl.config)
(alter-var-root #'refactor-nrepl.config/*config*
                assoc
                :print-right-margin nil
                :print-miser-width nil)

(future
  @refactor-nrepl.ns.class-search/available-classes-by-last-segment
  (@@#'refactor-nrepl.middleware/resolve-missing {:symbol "FileChannel"}))

(when (try
        (clojure.core/require 'cisco.tools.namespace.parallel-refresh)
        true
        (catch Exception _
          false))
  (alter-var-root (resolve 'clojure.tools.namespace.repl/refresh)
                  (fn [& _]
                    ;; Don't @ it - better to leave it redefable
                    (resolve 'cisco.tools.namespace.parallel-refresh/refresh))))
(clojure.core/require 'formatting-stack.core)
(clojure.core/require 'formatting-stack.branch-formatter)
(clojure.core/require 'formatting-stack.project-formatter)
(clojure.core/require 'formatting-stack.strategies)
(clojure.core/require 'clojure.test)
(clojure.core/require 'clojure.string)
(clojure.core/require 'clojure.reflect)

(require 'eastwood.linters.implicit-dependencies)
(alter-var-root (resolve 'eastwood.linters.implicit-dependencies/var->ns-symbol)
                (constantly (fn [var]
                              (-> var symbol namespace symbol))))

(clojure.core/eval '(clojure.core/create-ns 'vemv-warm))
(clojure.core/eval '(clojure.core/intern 'vemv-warm
                                         ;; a linting function apt for a broader selection of projects.
                                         'lint!
                                         (fn [& [full?]]
                                           #_ (formatting-stack.core/format!
                                               :formatters []
                                               :in-background? false
                                               ;; better no eastwood in unknown projects.
                                               :linters [#_ (-> (formatting-stack.linters.eastwood/new {})
                                                                (assoc :strategies (conj (if full?
                                                                                           formatting-stack.project-formatter/default-strategies
                                                                                           formatting-stack.defaults/extended-strategies)
                                                                                         formatting-stack.strategies/exclude-cljs
                                                                                         formatting-stack.strategies/jvm-requirable-files
                                                                                         formatting-stack.strategies/namespaces-within-refresh-dirs-only)))]))))

(require 'nedap.utils.collections.eager)

(defn find-in-dir [pred dir]
  (->>  dir
        io/file
        file-seq
        (nedap.utils.collections.eager/partitioning-pmap (fn [f]
                                                           (when ((every-pred (fn [^java.io.File f]
                                                                                (and (-> f .exists)
                                                                                     (not (-> f .isHidden))))
                                                                              pred
                                                                              (complement refactor-nrepl.core/build-artifact?))
                                                                  f)
                                                             f)))
        (filter identity)))

(defn find-in-project [pred]
  (->> (refactor-nrepl.core/dirs-on-classpath)
       (nedap.utils.collections.eager/partitioning-pmap (partial find-in-dir pred))
       (apply concat)
       distinct))

(defn fs-min! []
  (when (resolve 'formatting-stack.branch-formatter/default-formatters)
    (let [target-branch "master"
          f (fn [& {:as options}]
              (medley.core/mapply formatting-stack.strategies/git-diff-against-default-branch (assoc options :target-branch target-branch)))
          s1 [f
              formatting-stack.strategies/git-completely-staged]
          s2 [f
              formatting-stack.strategies/git-completely-staged
              formatting-stack.strategies/git-not-completely-staged]
          get-out (cond-> #{:formatting-stack.formatters.cljfmt/id}
                    (-> (System/getProperty "user.dir") (.contains "clash"))
                    (conj :formatting-stack.formatters.clean-ns/id))
          f (->> (@(resolve 'formatting-stack.branch-formatter/default-formatters) s1)
                 (remove (comp get-out :id))
                 (map (fn [{:keys [id] :as m}]
                        (case id
                          :formatting-stack.formatters.clean-ns/id
                          (update m
                                  :libspec-whitelist
                                  conj
                                  "matcher-combinators.test"
                                  "manifold.executor"
                                  "ring.core.spec")

                          m))))
          get-out (cond-> #{:formatting-stack.linters.ns-aliases/id
                            :formatting-stack.linters.line-length/id
                            :formatting-stack.linters.loc-per-ns/id}
                    (or (-> (System/getProperty "user.dir") (.contains "eastwood"))
                        (-> (System/getProperty "user.dir") (.contains "clash")))
                    (conj :formatting-stack.linters.eastwood/id
                          :formatting-stack.linters.one-resource-per-ns/id))
          l (->> (@(resolve 'formatting-stack.branch-formatter/default-linters) s2)
                 (remove (comp get-out :id)))]
      #_ (formatting-stack.core/format! :formatters f
                                        :linters []
                                        :in-background? false)
      ;; avoid dangling classes (defrecord/defprotocol):
      ;; (needs splitting fs-min! per refresh/reset)
      ;; (also needs to be performed *before* refreshing)
      #_ (when com.stuartsierra.component.repl/system
           (com.stuartsierra.component.repl/stop))
      #_ (formatting-stack.core/format! :formatters []
                                        :linters l
                                        ;; linting needs to be serial, else orchestra can be affected:
                                        :in-background? false)))

  (when (-> (System/getProperty "user.dir") (.contains "clash"))
    (some-> (resolve 'orchestra.spec.test/instrument)
            deref
            .call)))
