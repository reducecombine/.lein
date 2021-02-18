(ns vemv.nrepl
  (:require
   [clojure.tools.namespace.repl]
   [cisco.tools.namespace.parallel-refresh]
   [rebel-readline.clojure.line-reader]
   [rebel-readline.clojure.main]
   [rebel-readline.core])
  (:gen-class)
  (:import
   (java.io File PrintStream)
   (org.apache.commons.io.input Tailer TailerListenerAdapter)))

(try
  (require 'clojure.tools.nrepl) ;; explicit require so that refactor-nrepl can discover it
  (catch Exception _
    ;; most people don't use c.t.nrepl
    ))

;; allows Ctrl+C to interrupt long running tasks
(defn handle-sigint-form []
  `(let [thread# (Thread/currentThread)]
     (clojure.repl/set-break-handler! (fn [_signal#]
                                        (-> thread# .stop)))))

(defn start!* [& [skip-reset?]]
  ;; Make it possible to use the legacy c.t.nrepl - which some people might need:
  (let [port (or (some-> "NREPL_PORT" System/getenv Long/parseLong)
                 (+ 20000 (rand-int 20000)))
        start-server (or (requiring-resolve 'clojure.tools.nrepl.server/start-server)
                         (requiring-resolve 'nrepl.server/start-server))
        stop-server (or (requiring-resolve 'clojure.tools.nrepl.server/stop-server)
                        (requiring-resolve 'nrepl.server/stop-server))

        n "vemv.nrepl.outside-refresh-dirs"
        v "server"
        fqn (symbol n v)
        wrap-refactor (some-> 'refactor-nrepl.middleware/wrap-refactor requiring-resolve deref)
        handler (cond-> @(requiring-resolve 'cider.nrepl/cider-nrepl-handler)
                  wrap-refactor wrap-refactor)
        base-lein-command (format "lein repl :connect %s" port)
        lein-command (format "cd; LEIN_SILENT=true %s" base-lein-command)
        logfile (File. "log/dev.log")
        large-project? (or (-> logfile .exists)
                           (or (-> "user.dir" System/getProperty (.contains "/iroh"))
                               (-> "user.dir" System/getProperty (.contains "/ctia"))))
        ^PrintStream system-out System/out] ;; keep logging in iTerm - not the nREPL-connected IDE repl

    (create-ns (symbol n))

    (when-not (resolve fqn)

      (-> logfile .delete)

      (intern (symbol n)
              (symbol v)
              (start-server :port port :handler handler))

      (-> (Runtime/getRuntime) (.addShutdownHook (Thread. (fn []
                                                            (some-> 'user/stop resolve .invoke)))))
      (-> (Runtime/getRuntime) (.addShutdownHook (Thread. (fn []
                                                            (shutdown-agents)))))
      (-> (Runtime/getRuntime) (.addShutdownHook (Thread. (fn []
                                                            (some-> 'dev/stop resolve .invoke)))))
      (-> (Runtime/getRuntime) (.addShutdownHook (Thread. (fn []
                                                            (some-> 'formatting-stack.background/runner
                                                                    resolve
                                                                    deref
                                                                    future-cancel)))))
      (-> (Runtime/getRuntime) (.addShutdownHook (Thread. (fn []
                                                            (try
                                                              (some-> fqn resolve stop-server)
                                                              (catch NullPointerException _))))))

      (when large-project?
        (Tailer/create logfile
                       (proxy [TailerListenerAdapter] []
                         (handle [line]
                           (-> system-out (.println line))))
                       50))

      (when-not skip-reset?
        (try
          (some-> (or (-> 'user/go resolve)
                      (-> 'dev/reset resolve)
                      (-> 'user/reset resolve))
                  .invoke)
          (catch Throwable e
            (-> e .printStackTrace))))

      ((requiring-resolve 'clipboard.core/spit) lein-command)
      (spit ".nrepl-port" (str port "\n"))

      (when large-project?
        (println (format "Ready. Remember that Ctrl-C will terminate the JVM!\n  →  `%s` has been copied to the clipboard."
                         base-lein-command))))

    (if large-project?
      ;; avoid ugly `nil`:
      (symbol "")
      (rebel-readline.core/ensure-terminal
       (rebel-readline.clojure.main/repl*
        {:init (fn []
                 (when (find-ns 'dev)
                   (in-ns 'dev)))
         :eval (fn [form]
                 (eval `(do ~(handle-sigint-form) ~form)))})))))

(defn start!
  "Meant for usage from a terminal.
  (having the JVM run in a terminal is nice, as it can survive any IDE crashes)

  Loads all the project's code, then starts an nREPL.

  Does not start a repl in the terminal itself (as it's problematic).

  Tails `log/dev.log` so that this terminal tab is doing something visually useful.

  Note that Ctrl-C will kill the whole JVM
  (as is standard unix and `clj` behavior. 'Fixing' this brings complexities from/to nREPL)."
  []

  (-> *compile-path* java.io.File. .mkdirs)

  (binding [*compile-files* false] ;; works but hits usual AOT pitfalls in big apps (defprotocol, potemkin)
    (require 'vemv.emacs-backend)
    ;; Set the refresh dirs:
    (when (-> (java.io.File. "dev" "user.clj")
              .exists)
      (require 'user))

    (when (-> (java.io.File. "dev" "dev.clj")
              .exists)
      (require 'dev))
    (when-not (seq clojure.tools.namespace.repl/refresh-dirs)
      (clojure.tools.namespace.repl/set-refresh-dirs "src" "dev" "test"))

    ;; idea: don't compile any ns that (transitively) depends on schema, potemkin, trapperkeeper, c.t.logging
    (when (-> "user.dir" System/getProperty (.contains "iroh"))
      (cisco.tools.namespace.parallel-refresh/compile-3rd-party-deps!))

    (let [v (cisco.tools.namespace.parallel-refresh/refresh :after `start!*)]
      (when-not (#{:ok} v)
        (start!* :skip-reset)))))

(defn -main [& _]
  (start!))
