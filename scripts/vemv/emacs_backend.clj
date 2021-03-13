(ns vemv.emacs-backend
  "Performs `require`s that my Emacs setup assumes."
  (:require
   [clojure.java.io :as io]
   [vemv.usages]
   [vemv]))

(try
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
(clojure.core/require 'net.vemv.nrepl-debugger)
(clojure.core/require 'clj-java-decompiler.core)
(clojure.core/require 'lambdaisland.deep-diff)
(clojure.core/require 'criterium.core)
(clojure.core/require 'clojure.tools.namespace.repl)
(clojure.core/require 'com.stuartsierra.component.repl)
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
                                           (formatting-stack.core/format!
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

;; Adds formatting-stack.strategies.impl/readable? + parallelism
(alter-var-root #'refactor-nrepl.ns.libspecs/namespace-aliases
                (constantly
                 (fn vemv--namespace-aliases []
                   (let [aliases-by-frequencies  #'refactor-nrepl.ns.libspecs/aliases-by-frequencies
                         get-libspec-from-file-with-caching #'refactor-nrepl.ns.libspecs/get-libspec-from-file-with-caching]
                     {:clj  (->> (find-in-project (every-pred (fn [f-or-s]
                                                                (cond-> f-or-s
                                                                  (instance? java.io.File f-or-s) (.getAbsolutePath)
                                                                  true                            formatting-stack.strategies.impl/readable?))
                                                              (some-fn refactor-nrepl.core/clj-file? refactor-nrepl.core/cljc-file?)))
                                 (nedap.utils.collections.eager/partitioning-pmap (partial get-libspec-from-file-with-caching :clj))
                                 aliases-by-frequencies)
                      :cljs (if true
                              [] ;; save some time since I'm not doing cljs atm
                              (->> (find-in-project (some-fn refactor-nrepl.core/cljs-file? refactor-nrepl.core/cljc-file?))
                                   (nedap.utils.collections.eager/partitioning-pmap (partial get-libspec-from-file-with-caching :cljs))
                                   aliases-by-frequencies))}))))
