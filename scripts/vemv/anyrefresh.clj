(ns vemv.anyrefresh
  "Sets up tools.namespace in any project in an idempotent manner
  (i.e. already set-up projects will remain untouched).

  It also warms the 'AST cache' for refactor-nrepl in a way that is integrated with the mentioned setup."
  (:require
   [clojure.tools.namespace.repl]
   [com.stuartsierra.component.repl]))

;; the eval business prevents `refactor-nrepl.analyzer/warm-ast-cache` from throwing "not found"

(eval '(create-ns 'vemv-warm))

;; NOTE: requiring 'dev or 'user foils tools.namespace.parallel-refresh/compile-3rd-party-deps! if
;; 'dev or 'user require the main codebase.
;; currently it doesn't happen in my main work codebase.
;; in many projects it happens.
;; I should revisit my dev pattern. a dev vs. user separation might be due.

;; (idea: parse user.clj / dev.clj without requiring those namespaces.)

(eval '(try
         ;; Avoid `require`ing `dev` if it's not in this project
         ;; (given lein checkouts can bring extraneous dev nses)
         (when (->> ["dev/dev.clj"
                     "src/dev.clj"]
                    (some (fn [^String filename]
                            (-> filename java.io.File. .exists))))
           ;; Don't require dev - can foil tools.namespace.parallel-refresh/compile-3rd-party-deps!
           #_ (require 'dev))

         ;; maybe there was a namespaces called `dev`, but without any t.n setup:
         (when-not (seq clojure.tools.namespace.repl/refresh-dirs)
           (throw (ex-info "" {::no-print true})))

         (catch Exception e ;; no `dev` ns, or `dev` w/o t.n.setup
           (when-not (-> e ex-data ::no-print)
             (-> e .printStackTrace))
           (try
             ;; Avoid `require`ing `user` if it's not in this project
             ;; (given lein checkouts can bring extraneous dev nses)
             (when (->> ["dev/user.clj"
                         "src/user.clj"]
                        (some (fn [^String filename]
                                (-> filename java.io.File. .exists))))
               ;; Don't require `user` - can foil tools.namespace.parallel-refresh/compile-3rd-party-deps!
               #_ (require 'user))
             (catch Exception _))

           (when-not (seq clojure.tools.namespace.repl/refresh-dirs)
             (->> ["dev"
                   "libs"
                   "modules"
                   (when-not (-> "src/main/clojure"
                                 java.io.File.
                                 .exists)
                     "src")
                   (when-not (-> "test/main/clojure"
                                 java.io.File.
                                 .exists)
                     "test")
                   "main"
                   "clojure"
                   "src/main/clojure"
                   "src/test/clojure"
                   ;; Eastwood:
                   "test-third-party-deps"]
                  (filter (fn [^String x]
                            (some-> x java.io.File. .exists)))
                  (apply clojure.tools.namespace.repl/set-refresh-dirs))))))

(eval '(intern 'vemv-warm
               'vemv-warm
               (delay
                 ;; guard against file with intentionally broken syntax:
                 (when-not (-> "user.dir" System/getProperty (.contains "/formatting-stack"))
                   (refactor-nrepl.analyzer/warm-ast-cache)))))

(eval '(intern 'vemv-warm
               'init-fn
               com.stuartsierra.component.repl/initializer))

;; commented out - foils tools.namespace.parallel-refresh/compile-3rd-party-deps! :
#_
(eval '(intern
        'vemv-warm
        'vemv-do-warm
        (fn []
          (if (eval '(= vemv-warm/init-fn
                        com.stuartsierra.component.repl/initializer))
            ;; ... this means `(set-init)` has not been invoked, which means that this project does not use Sierra's `reset`:
            (do
              @vemv-warm/vemv-warm
              ;; invoke home-grown `reset` functions, e.g. https://git.io/Jff6j :
              (some-> 'user/reset resolve .invoke))
            (when-let [v (try
                           (eval '(com.stuartsierra.component.repl/reset))
                           (future ;; wrap in a future - it is assumed projects with a System can be large:
                             @vemv-warm/vemv-warm)
                           (catch java.lang.Throwable v
                             (when (instance? java.io.FileNotFoundException v)
                               (eval '(clojure.tools.namespace.repl/clear)))
                             (when (com.stuartsierra.component/ex-component? v)
                               (some-> v
                                       ex-data
                                       :system
                                       com.stuartsierra.component/stop))
                             v))]
              (when (instance? java.lang.Throwable v)
                (when (instance? java.io.FileNotFoundException v)
                  (eval '(clojure.tools.namespace.repl/clear)))
                (-> v .printStackTrace)))))))

;; commented out - foils tools.namespace.parallel-refresh/compile-3rd-party-deps! :
#_ (let [v (eval '((or (resolve 'cisco.tools.namespace.parallel-refresh/refresh)
                       (resolve 'clojure.tools.namespace.repl/refresh)) :after 'vemv-warm/vemv-do-warm))]
     (when (instance? java.lang.Exception v)
       (println v)))

;; particularly useful for projects without a `t.n` setup whatsoever
(let [used (->> *ns* ns-refers keys set)]
  (->> '[clear refresh refresh-dirs set-refresh-dirs]
       (remove used)
       (vec)
       (list 'clojure.tools.namespace.repl :only)
       (apply refer)))
