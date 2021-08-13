(ns vemv.usages
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.pprint :refer [pprint]]
   [formatting-stack.core]
   [formatting-stack.linters.kondo]
   [formatting-stack.kondo-classpath-cache]
   [formatting-stack.strategies :as strategies]
   [clojure.string :as string]))

(defn project-files []
  (formatting-stack.core/files-from-strategies
   [strategies/all-files
    strategies/jvm-requirable-files
    strategies/namespaces-within-refresh-dirs-only]))

(defn usages [named-thing]
  @formatting-stack.kondo-classpath-cache/classpath-cache
  (let [named-thing (cond-> named-thing
                      (var? named-thing) symbol)
        lint (project-files)
        k? (keyword? named-thing)
        {{:keys [var-usages keywords]} :analysis} (clj-kondo/run! {:config   {:output  {:analysis {:keywords k?}}
                                                                              :lint-as (:lint-as formatting-stack.linters.kondo/default-options)}
                                                                   :parallel true
                                                                   :lint     lint})
        ns-to-find (-> named-thing namespace symbol)
        name-to-find (cond-> named-thing
                       true name
                       (not k?) symbol)]
    (->> (if k?
           keywords
           var-usages)
         (keep (fn [{:keys [ns to name filename col row] :as v}]
                 (when (and (= (if k?
                                 ns
                                 to)
                               ns-to-find)
                            (= name name-to-find))
                   (string/replace (str filename ":" row ":" col)
                                   (str (System/getProperty "user.dir") "/")
                                   ""))))
         (run! println))))
