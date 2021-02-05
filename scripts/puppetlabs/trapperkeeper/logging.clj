(ns puppetlabs.trapperkeeper.logging
  "Removes the dependency on logback which has the potential to confuse sl4j discovery"
  (:import #_ [ch.qos.logback.classic Level PatternLayout]
           #_ (ch.qos.logback.core ConsoleAppender)
           (org.slf4j Logger LoggerFactory)
           #_ (ch.qos.logback.classic.joran JoranConfigurator))
  (:require [clojure.stacktrace :refer [print-cause-trace]]
            [clojure.tools.logging :as log]
            [puppetlabs.i18n.core :as i18n]))

(defn logging-context
  []
  (LoggerFactory/getILoggerFactory))

(defn reset-logging
  []
  (.reset (logging-context)))

(def root-logger-name Logger/ROOT_LOGGER_NAME)

(defn root-logger
  []
  (LoggerFactory/getLogger root-logger-name))

(defn catch-all-logger
  "A logging function useful for catch-all purposes, that is, to
  ensure that a log message gets in front of a user the best we can
  even if that means duplicated output.

  This is really only suitable for _last-ditch_ exception handling,
  where we want to make sure an exception is logged (because nobody
  higher up in the stack will log it for us)."
  ([exception]
   (catch-all-logger exception (i18n/trs "Uncaught exception")))
  ([exception message]
   (print-cause-trace exception)
   (flush)
   (log/error exception message)))

#_ (defn create-console-appender
     "Instantiates and returns a logging appender configured to write to
  the console, using the standard logging configuration.

  `level` is an optional argument (of type `org.apache.log4j.Level`)
  indicating the logging threshold for the new appender.  Defaults
  to `DEBUG`."
     ([]
      (create-console-appender Level/DEBUG))
     ([level]
      {:pre [(instance? Level level)]}
      (let [layout (PatternLayout.)]
        (doto layout
          (.setContext (logging-context))
          (.setPattern "%d %-5p [%t] [%c{2}] %m%n")
          (.start))
        (doto (ConsoleAppender.)
          (.setContext (logging-context))
          (.setLayout layout)
          (.start)))))

#_ (defn add-console-logger!
     "Adds a console logger to the current logging configuration, and ensures
  that the root logger is set to log at the logging level of the new
  logger or finer.

  `level` is an optional argument (of type `org.apache.log4j.Level`)
  indicating the logging threshold for the new logger.  Defaults
  to `DEBUG`."
     ([]
      (add-console-logger! Level/DEBUG))
     ([level]
      {:pre [(instance? Level level)]}
      (let [root (root-logger)]
        (.addAppender root (create-console-appender level))
        (if (> (.toInt (.getLevel root))
               (.toInt level))
          (.setLevel root level)))))

#_ (defn configure-logger!
     "Reconfigures the current logger based on the supplied configuration.

  Supplied configuration can be a file path, url, file, InputStream, or
  InputSource. It is passed along unchanged to `doConfigure` for
  JoranConfigurator. For more information, see the documentation for
  ch.qos.logback.core.classic.joran.JoranConfigurator."
     [logging-conf]
     (let [configurator (JoranConfigurator.)
           context      (LoggerFactory/getILoggerFactory)]
       (.setContext configurator (LoggerFactory/getILoggerFactory))
       (.reset context)
       (.doConfigure configurator logging-conf)))

(defn configure-logging!
  "Takes a file path, url, file, InputStream, or InputSource which can
  define how to configure the logging system. This is passed unchanged
  to the `doConfigure` method for the underlying JoranConfigurator
  class.

  Also takes an optional `debug` flag which turns on debug logging."
  ([logging-conf]
   (configure-logging! logging-conf false))
  ([logging-conf debug]
   (when logging-conf
     #_ (configure-logger! logging-conf))
   (when debug
     #_ (add-console-logger! Level/DEBUG)
     (log/debug (i18n/trs "Debug logging enabled")))))
