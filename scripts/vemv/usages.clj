(ns vemv.usages
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.pprint :refer [pprint]]
   [formatting-stack.core]
   [formatting-stack.kondo-classpath-cache]
   [formatting-stack.strategies :as strategies]
   [clojure.string :as string]))

(defn project-files []
  (formatting-stack.core/files-from-strategies
   [strategies/all-files
    strategies/jvm-requirable-files
    strategies/namespaces-within-refresh-dirs-only]))

(defn usages [var-name]
  @formatting-stack.kondo-classpath-cache/classpath-cache
  (let [lint (project-files)
        {{:keys [var-usages]} :analysis} (clj-kondo/run! {:config   {:output {:analysis true}}
                                                          :parallel true
                                                          :lint     lint})
        ns-to-find (-> var-name namespace symbol)
        name-to-find (-> var-name name symbol)]
    (->> var-usages
         (keep (fn [{:keys [to name filename col row] :as v}]
                 (when (and (= to ns-to-find)
                            (= name name-to-find))
                   (string/replace (str filename ":" row ":" col)
                                   (str (System/getProperty "user.dir") "/")
                                   ""))))
         (run! println))))
