{:user               {:plugins                  [[lein-pprint "1.1.2"]
                                                 [lein-subscribable-urls "0.1.0-alpha2"]
                                                 [lein-lein "0.2.0"]]
                      :dependencies             [[lein-subscribable-urls "0.1.0-alpha2"]]
                      :jvm-opts                 ["-Dapple.awt.UIElement=true"
                                                 "-Dclojure.compiler.disable-locals-clearing=true"
                                                 "-Dclojure.core.async.go-checking=true"
                                                 "-Dclojure.main.report=stderr"
                                                 "-Dformatting-stack.eastwood.parallelize-linters=true"
                                                 "-server"
                                                 "-Dpepkey.running_from_docker=false"
                                                 "-XX:-OmitStackTraceInFastThrow"
                                                 "-XX:+TieredCompilation"
                                                 "-XX:TieredStopAtLevel=1"
                                                 "-Xmx18G"
                                                 "-Xverify:none" #_"Improves perf"]
                      :monkeypatch-clojure-test false}

 ;; the following profile serves for two use cases:
 ;; * Launching `lein repl` from iTerm
 ;; * Launching an in-Emacs JVM
 ;; Perhaps for the latter, the :plugins section is redundant. Hasn't given problems so far.
 :emacs-backend      {:dependencies   [[cider/cider-nrepl "0.16.0"]
                                       [criterium "0.4.5"]
                                       [formatting-stack "4.2.0"]
                                       [medley "1.2.0"]
                                       [lambdaisland/deep-diff "0.0-29"]
                                       [org.clojure/clojure "1.10.1"]
                                       [org.clojure/tools.reader "1.1.1"]
                                       [org.clojure/java.jmx "0.3.4"]
                                       [com.clojure-goes-fast/clj-java-decompiler "0.2.1"]
                                       [org.clojure/spec.alpha "0.2.176"]
                                       [org.clojure/tools.nrepl "0.2.13"]
                                       [org.clojure/tools.namespace "0.3.1"]
                                       [nrepl-debugger "0.1.0-SNAPSHOT"]
                                       [com.nedap.staffing-solutions/utils.collections "2.1.0"]
                                       [com.stuartsierra/component.repl "0.2.0"]]

                      ;; http://rebl.cognitect.com/download.html
                      :resource-paths ["/Users/vemv/.lein/resources/rebl.jar"]

                      :plugins        [[refactor-nrepl "2.4.0" :exclusions [org.clojure/tools.logging]]
                                       [cider/cider-nrepl "0.16.0"]]
                      :repl-options   {:port    41235
                                       :timeout 600000
                                       :welcome "Print nothing"
                                       :init    {:emacs-backend
                                                 (do
                                                   (clojure.core/require 'refactor-nrepl.core)
                                                   (clojure.core/require 'refactor-nrepl.middleware)
                                                   (clojure.core/require 'refactor-nrepl.analyzer)
                                                   (clojure.core/require 'net.vemv.nrepl-debugger)
                                                   (clojure.core/require 'clj-java-decompiler.core)
                                                   (clojure.core/require 'lambdaisland.deep-diff)
                                                   (clojure.core/require 'criterium.core)
                                                   (clojure.core/require 'formatting-stack.core)
                                                   (clojure.core/require 'formatting-stack.branch-formatter)
                                                   (clojure.core/require 'formatting-stack.project-formatter)
                                                   (clojure.core/require 'clojure.test)
                                                   (clojure.core/require 'clojure.tools.namespace.repl)
                                                   (clojure.core/require 'com.stuartsierra.component.repl))}}}

 :terminal           {:repl-options {:port 41233}}

 :yourkit            {:jvm-opts ["-agentpath:/Applications/YourKit-Java-Profiler-2019.8.app/Contents/Resources/bin/mac/libyjpagent.dylib"]}

 :emacs-backend-init {:repl-options {:init {:emacs-backend-init
                                            (do
                                              (clojure.core/require 'clojure.tools.namespace.repl)
                                              (clojure.core/require 'com.stuartsierra.component.repl)
                                              ;; the eval business prevents `refactor-nrepl.analyzer/warm-ast-cache` from throwing "not found"
                                              (clojure.core/alter-var-root #'clojure.test/*load-tests* (clojure.core/constantly false))
                                              (clojure.core/eval '(clojure.core/create-ns 'vemv-warm))
                                              (clojure.core/eval '(try
                                                                    ;; Avoid `require`ing `dev` if it's not in this project
                                                                    ;; (given lein checkouts can bring extraneous dev nses)
                                                                    (when (->> ["dev/dev.clj"
                                                                                "src/dev.clj"]
                                                                               (some (fn [^String filename]
                                                                                       (-> filename
                                                                                           java.io.File.
                                                                                           .exists))))
                                                                      (require 'dev))

                                                                    ;; maybe there was a namespaces called `dev`, but without any t.n setup:
                                                                    (when-not (seq clojure.tools.namespace.repl/refresh-dirs)
                                                                      (throw (ex-info "." {})))

                                                                    (catch Exception e ;; no `dev` ns, or `dev` w/o t.n.setup
                                                                      #_ (clojure.core/-> e .printStackTrace)
                                                                      (try
                                                                        ;; Avoid `require`ing `user` if it's not in this project
                                                                        ;; (given lein checkouts can bring extraneous dev nses)
                                                                        (when (->> ["dev/user.clj"
                                                                                    "src/user.clj"]
                                                                                   (some (fn [^String filename]
                                                                                           (-> filename
                                                                                               java.io.File.
                                                                                               .exists))))
                                                                          (require 'user))
                                                                        (catch Exception _))

                                                                      (when-not (seq clojure.tools.namespace.repl/refresh-dirs)
                                                                        (->> ["dev"
                                                                              "libs"
                                                                              "modules"
                                                                              (when-not (-> "src/main/clojure"
                                                                                            java.io.File.
                                                                                            .exists)
                                                                                "src")
                                                                              "test"
                                                                              "main"
                                                                              "clojure"
                                                                              "src/main/clojure"
                                                                              "src/test/clojure"]
                                                                             (filter (fn [^String x]
                                                                                       (some-> x java.io.File. .exists)))
                                                                             (apply clojure.tools.namespace.repl/set-refresh-dirs))))))
                                              (clojure.core/eval '(clojure.core/intern 'vemv-warm
                                                                                       'vemv-warm
                                                                                       (clojure.core/delay
                                                                                         (refactor-nrepl.analyzer/warm-ast-cache))))
                                              (clojure.core/eval '(clojure.core/intern 'vemv-warm
                                                                                       'init-fn
                                                                                       com.stuartsierra.component.repl/initializer))
                                              (clojure.core/eval '(clojure.core/intern
                                                                   'vemv-warm
                                                                   'vemv-do-warm
                                                                   (fn []
                                                                     (if (eval '(clojure.core/= vemv-warm/init-fn
                                                                                                com.stuartsierra.component.repl/initializer))
                                                                       @vemv-warm/vemv-warm
                                                                       (clojure.core/when-let [v (try
                                                                                                   (eval '(com.stuartsierra.component.repl/reset))
                                                                                                   (future ;; wrap in a future - it is assumed projects with a System can be large:
                                                                                                     @vemv-warm/vemv-warm)
                                                                                                   (catch java.lang.Throwable v
                                                                                                     (clojure.core/when (clojure.core/instance? java.io.FileNotFoundException v)
                                                                                                       (eval '(clojure.tools.namespace.repl/clear)))
                                                                                                     (clojure.core/when (com.stuartsierra.component/ex-component? v)
                                                                                                       (clojure.core/some-> v
                                                                                                                            clojure.core/ex-data
                                                                                                                            :system
                                                                                                                            com.stuartsierra.component/stop))
                                                                                                     v))]
                                                                         (clojure.core/when (clojure.core/instance? java.lang.Throwable v)
                                                                           (clojure.core/when (clojure.core/instance? java.io.FileNotFoundException v)
                                                                             (eval '(clojure.tools.namespace.repl/clear)))
                                                                           (clojure.core/-> v .printStackTrace)))))))

                                              (clojure.core/eval '(clojure.tools.namespace.repl/refresh :after 'vemv-warm/vemv-do-warm))

                                              ;; particularly useful for projects without a `t.n` setup whatsoever
                                              (let [used (->> *ns* ns-refers keys set)]
                                                (->> '[clear refresh refresh-dirs set-refresh-dirs]
                                                     (remove used)
                                                     (vec)
                                                     (list 'clojure.tools.namespace.repl :only)
                                                     (apply clojure.core/refer))))}}}

 :nedap-gw           {:repl-options {:port 41237}}

 :nedap-link         {:source-paths ["src/test"]
                      :repl-options {:port    41236
                                     :timeout 600000
                                     :init-ns dev}}

 :nedap-key          {:source-paths ["specs/server"]
                      :jvm-opts     ["-Dlogback.configurationFile=logback-no-stdout.xml"]
                      :repl-options {:port    41234
                                     :timeout 600000
                                     :init-ns dev}}

 :refactor-nrepl     {:dependencies [[http-kit "2.3.0"]
                                     [cheshire "5.8.0"]
                                     [org.clojure/tools.analyzer.jvm "0.7.1"]
                                     [org.clojure/tools.namespace "0.3.0-alpha3" :exclusions [org.clojure/tools.reader]]
                                     [org.clojure/tools.reader "1.1.1"]
                                     [cider/orchard "0.3.0"]
                                     [cljfmt "0.6.3"]
                                     [me.raynes/fs "1.4.6"]
                                     [rewrite-clj "0.6.0"]
                                     [cljs-tooling "0.2.0"]
                                     [version-clj "0.1.2"]]}

 :emacs-figwheel     {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                     [figwheel-sidecar "0.5.16"]]
                      :plugins      [[cider/cider-nrepl "0.16.0"]]
                      :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                      :figwheel     {:nrepl-middleware ["cider.nrepl/cider-middleware"
                                                        "refactor-nrepl.middleware/wrap-refactor"
                                                        "cemerick.piggieback/wrap-cljs-repl"]}}

 :eftest             {:plugins [[lein-eftest "0.5.8"]]
                      :eftest  {:multithread? false
                                :fail-fast?   true}}}
