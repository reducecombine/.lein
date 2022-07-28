(ns formatting-stack.linters.eastwood
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [eastwood.lint]
   [formatting-stack.linters.eastwood.impl :as impl]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.protocols.spec :as protocols.spec]
   [formatting-stack.util :refer [ns-name-from-filename silence]]
   [medley.core :refer [assoc-some deep-merge]]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]
   [nedap.utils.spec.api :refer [check!]])
  (:import
   (java.io File)
   (java.util.concurrent TimeUnit)
   (java.util.concurrent.locks ReentrantLock)))

(def default-eastwood-options
  (-> eastwood.lint/default-opts
      (assoc :rethrow-exceptions? true)))

(defn run-eastwood [options reports]
  (eastwood.lint/eastwood options (impl/->TrackingReporter reports)))

(defn wrap-with-locking
  "If using a 'refresh' library featuring a global lock, uses said lock to prevent concurrent code evaluation
  between the library and Eastwood.

  For example, calling `(refresh :after format!)` twice in a row, where `format!` uses `:in-background? true`,
  can be otherwise dangerous."
  [f]
  (fn [options reports]
    (speced/let [var-sym 'cisco.tools.namespace.parallel-refresh/refresh-lock
                 ^ReentrantLock lock @(resolve var-sym)
                 seconds 60]
      (if-not (-> lock (.tryLock seconds TimeUnit/SECONDS))
        (println (str "Could not acquire " var-sym " after" seconds " seconds. The Eastwood linter will not be executed."))
        (try
          (f options reports)
          (finally
            (-> lock .unlock)))))))

(defn eastwood-runner [] ;; this a defn since the result of `find-ns` can vary
  (cond-> run-eastwood
    ;; Only cisco.tools.namespace.parallel-refresh features a global lock
    ;; (if clojure.tools.namespace.repl featured one, it would be nice and just as necessary to add it here. But it doesn't).
    (find-ns 'cisco.tools.namespace.parallel-refresh) (wrap-with-locking)))

(defn lint! [{:keys [options]} filenames]
  {:post [(do
            (assert (check! (speced/fn [^::protocols.spec/reports xs]
                              (let [output (->> xs (keep :filename) (set))]
                                (set/subset? output (set filenames))))
                            %)
                    "The `:filename`s returned from Eastwood should be a subset of this function's `filenames`.
Otherwise, it would mean that our filename absolutization out of Eastwood reports is buggy.")
            true)]}
  (let [namespaces (->> filenames
                        (remove #(str/ends-with? % ".edn"))
                        (keep ns-name-from-filename))
        reports    (atom nil)
        exceptions (atom nil)]

    (silence
     (try
       (-> options
           (assoc :namespaces namespaces)
           ((eastwood-runner) reports))
       (catch Exception e
         (swap! exceptions conj e))))
    (->> @reports
         :warnings
         (map :warn-data)
         (map (fn [{:keys [uri-or-file-name linter] :strs [warning-details-url] :as m}]
                (assoc-some m
                            :level               :warning
                            :source              (keyword "eastwood" (name linter))
                            :warning-details-url warning-details-url
                            :filename            (speced/let [^::speced/nilable ^String s (when (string? uri-or-file-name)
                                                                                            uri-or-file-name)
                                                              ^File file (or (some-> s File.)
                                                                             uri-or-file-name)]
                                                   (-> file .getCanonicalPath)))))
         (into (impl/exceptions->reports @exceptions)))))

(defn new [{:keys [eastwood-options]
            :or   {eastwood-options {}}}]
  (implement {:id ::id
              :options (deep-merge default-eastwood-options eastwood-options)}
    linter/--lint! lint!))
