(ns vemv.nrepl
  (:require
   [vemv.emacs-backend]
   [clojure.tools.namespace.repl]
   [cisco.tools.namespace.parallel-refresh]
   [rebel-readline.clojure.line-reader]
   [rebel-readline.clojure.main]
   [rebel-readline.core])
  (:gen-class)
  (:import
   (java.io File PrintStream)
   (org.apache.commons.io IOUtils)
   (org.apache.commons.io.input Tailer TailerListener)))

(require 'vemv.anyrefresh)

(try
  ;; explicit require so that refactor-nrepl can discover it
  ;; NOTE: this must be placed before any refactor-nrepl require.
  (require 'clojure.tools.nrepl)
  (catch Exception _
    ;; most people don't use c.t.nrepl
    ))

;; allows Ctrl+C to interrupt long running tasks
(defn handle-sigint-form []
  `(let [thread# (Thread/currentThread)]
     (clojure.repl/set-break-handler! (fn [_signal#]
                                        (-> thread# .stop)))))

(defn start!* [& [skip-reset?]]
  (try
    (-> cisco.tools.namespace.parallel-refresh/refresh-lock .unlock)
    (catch java.lang.IllegalMonitorStateException _))
  (let [port (or (some-> "NREPL_PORT" System/getenv Long/parseLong)
                 (+ 20000 (rand-int 20000)))
        ;; Make it possible to use the legacy c.t.nrepl - which some people might need:
        start-server (or (requiring-resolve 'clojure.tools.nrepl.server/start-server)
                         (requiring-resolve 'nrepl.server/start-server))
        stop-server (or (requiring-resolve 'clojure.tools.nrepl.server/stop-server)
                        (requiring-resolve 'nrepl.server/stop-server))

        n "vemv.nrepl.outside-refresh-dirs"
        v "server"
        fqn (symbol n v)
        wrap-refactor (when-not (-> "user.dir" System/getProperty (.contains "/refactor-nrepl"))
                        (some-> 'refactor-nrepl.middleware/wrap-refactor requiring-resolve deref))
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
                       (reify TailerListener
                         (^void handle [_ ^String line]
                          (-> system-out (.println line)))
                         (^void handle [_ ^Exception e]
                          (-> system-out (.println (->> e .getMessage)))
                          (-> system-out (.println (->> e .getStackTrace (clojure.string/join "\n")))))
                         (^void init [_ ^Tailer e])
                         (^void fileNotFound [_])
                         (^void fileRotated [_]))
                       50
                       false ;; tail from the beginning of the file.
                       true ;; reOpen https://issues.apache.org/jira/browse/IO-399
                       IOUtils/DEFAULT_BUFFER_SIZE))

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

  Tails `log/dev.log` so that this terminal tab is doing something visually useful."
  []

  ;; Set the refresh dirs:
  (when (-> (java.io.File. "dev" "user.clj")
            .exists)
    (require 'user))

  (when (-> (java.io.File. "dev" "dev.clj")
            .exists)
    (require 'dev))

  (when-not (seq clojure.tools.namespace.repl/refresh-dirs)
    (clojure.tools.namespace.repl/set-refresh-dirs "src" "dev" "test"))

  (cisco.tools.namespace.parallel-refresh/compile-3rd-party-deps!)

  (let [v (cisco.tools.namespace.parallel-refresh/refresh :after `start!*)]
    (when-not (#{:ok cisco.tools.namespace.parallel-refresh/ok-result-marker} v)
      (start!* :skip-reset))))

(defn -main [& _]
  (start!))

(do
  ;; https://github.com/clojure-emacs/cider-nrepl/pull/701
  (require 'cider.nrepl.middleware.stacktrace)
  (alter-var-root #'cider.nrepl.middleware.stacktrace/directory-namespaces disj 'dev 'user))
