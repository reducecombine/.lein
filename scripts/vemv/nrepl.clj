(ns vemv.nrepl
  (:gen-class)
  (:require
   [cider.nrepl :refer [cider-nrepl-handler]]
   [cisco.tools.namespace.parallel-refresh]
   [clipboard.core :as clipboard])
  (:import
   (java.io File PrintStream)
   (org.apache.commons.io.input Tailer TailerListenerAdapter)))

(defn start!* []
  ;; Make it possible to use the legacy c.t.nrepl - which some people might need:
  (let [port (or (some-> "NREPL_PORT" System/getenv Long/parseLong)
                 48372)
        start-server (or (requiring-resolve 'clojure.tools.nrepl.server/start-server)
                         (requiring-resolve 'nrepl.server/start-server))
        stop-server (or (requiring-resolve 'clojure.tools.nrepl.server/stop-server)
                         (requiring-resolve 'nrepl.server/stop-server))
        
        n "user.nrepl.outside-refresh-dirs"
        v "server"
        fqn (symbol n v)
        wrap-refactor (some-> 'refactor-nrepl.middleware/wrap-refactor requiring-resolve deref)
        handler (cond-> cider-nrepl-handler
                  wrap-refactor wrap-refactor)
        base-lein-command (format "lein repl :connect %s" port)
        lein-command (format "cd; LEIN_SILENT=true %s" base-lein-command)
        logfile (File. "log/dev.log")
        ^PrintStream system-out System/out] ;; keep logging in iTerm - not the nREPL-connected IDE repl

    (-> logfile .delete)

    (create-ns (symbol n))

    (when-not (resolve fqn)
      (intern (symbol n)
              (symbol v)
              (start-server :port port :handler handler))
      (-> (Runtime/getRuntime (.addShutdownHook (Thread. (fn []
                                                           (some-> 'user/stop resolve .invoke))))))
      (-> (Runtime/getRuntime (.addShutdownHook (Thread. (fn []
                                                           (some-> 'dev/stop resolve .invoke))))))
      (-> (Runtime/getRuntime (.addShutdownHook (Thread. (fn []
                                                           (-> fqn resolve stop-server)))))))

    (Tailer/create logfile
                   (proxy [TailerListenerAdapter] []
                     (handle [line]
                       (-> system-out (.println line))))
                   50)

    (some-> (or (-> 'user/go resolve)
                (-> 'dev/reset resolve)
                (-> 'user/reset resolve))
            .invoke)

    (clipboard/spit lein-command)

    (println (format "\nReady. Remember that Ctrl-C will terminate the JVM!\n  →  `%s` has been copied to the clipboard."
                     base-lein-command))

    ;; avoid ugly `nil`:
    (symbol "")))

(defn start!
  "Meant for usage from a terminal.
  (having the JVM run in a terminal is nice, as it can survive any IDE crashes)

  Loads all the project's code, then starts an nREPL.

  Does not start a repl in the terminal itself (as it's problematic).

  Tails `log/dev.log` so that this terminal tab is doing something visually useful.

  Note that Ctrl-C will kill the whole JVM
  (as is standard unix and `clj` behavior. 'Fixing' this brings complexities from/to nREPL)."
  []
  (cisco.tools.namespace.parallel-refresh/refresh :after `start!*))

(defn -main [& _]
  ;; Set the refresh dirs:
  (try (require 'user) (catch Exception _))
  (try (require 'dev) (catch Exception _))

  ;; invoke `refresh`:
  (start!))
