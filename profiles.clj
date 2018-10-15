{:user {:plugins [[lein-bikeshed "0.5.1" :exclusions [org.clojure/clojure]]
                  [lein-cljfmt "0.6.0" :exclusions [org.clojure/clojure org.clojure/tools.cli]]
                  [lein-pprint "1.1.2" :exclusions [org.clojure/clojure]]
                  [com.jakemccrary/lein-test-refresh "0.23.0" :exclusions [org.clojure/clojure
                                                                           org.clojure/tools.namespace]]
                  [nrepl/lein-nrepl "0.1.2" :exclusions [org.clojure/clojure]]
                  [lein-lein "0.2.0"]
                  [com.gfredericks/how-to-ns "0.1.9" :exclusions [org.clojure/clojure]]
                  [com.gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character
                   "0.1.1"
                   :exclusions
                   [org.clojure/clojure]]]
        :jvm-opts ["-Dapple.awt.UIElement=true"]
        :pedantic? :warn
        :how-to-ns {:require-docstring?      false
                    :sort-clauses?           true
                    :allow-refer-all?        false
                    :allow-extra-clauses?    false
                    :align-clauses?          false
                    :import-square-brackets? true}}
 :auth {:deploy-repositories [["releases" {:url "https://clojars.org/repo" :sign-releases false}]
                              ["snapshots" :clojars]]
        :repository-auth {#"https://clojars\.org/repo" {:username "vemv"
                                                        :password #=(some-> "CLOJARS_PASSWORD"
                                                                            System/getenv
                                                                            eval)}}}
 :emacs-docsolver-backend {:repl-options {:init (do
                                                  (require 'cider.nrepl.middleware.stacktrace)
                                                  ;; https://github.com/clojure-emacs/cider-nrepl/issues/547
                                                  (let [prev (-> *ns* ns-name)]
                                                    (in-ns 'cider.nrepl.middleware.stacktrace)
                                                    (eval '(defn flag-project
                                                             [{:keys [ns] :as frame}]
                                                             (if (and directory-namespaces ns
                                                                      (or (contains? directory-namespaces (symbol ns))
                                                                          (.startsWith ns "app.")
                                                                          (.startsWith ns "unit.")))
                                                               (flag-frame frame :project)
                                                               frame)))
                                                    (in-ns prev))
                                                  (user/reset))
                                          :port 45432
                                          :timeout 180000}
                           :source-paths ^:replace #=(some-> "DS_BACKOFFICE_BACKEND_DEV_SOURCE_PATHS"
                                                             System/getenv
                                                             read-string
                                                             eval)
                           :dependencies [[org.clojure/tools.nrepl "0.2.13" :exclusions [org.clojure/clojure]]]
                           :plugins [[refactor-nrepl "2.4.0" :exclusions [org.clojure/tools.logging]]
                                     [cider/cider-nrepl "0.16.0"]]}
 :emacs-docsolver-frontend {:dependencies [[lein-doo "0.1.10" :exclusions [org.clojure/clojure]]]
                            :source-paths ["test" "../common/test"]
                            :cljsbuild {:builds {:dev {:source-paths ["test" "../common/test"]}}}}
 :emacs-figwheel {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                 [figwheel-sidecar "0.5.16"]]
                  :plugins [[cider/cider-nrepl "0.16.0"]]
                  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                  :figwheel {:nrepl-middleware ["cider.nrepl/cider-middleware"
                                                "refactor-nrepl.middleware/wrap-refactor"
                                                "cemerick.piggieback/wrap-cljs-repl"]}}}
