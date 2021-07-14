{:user               {:plugins                  [[com.github.liquidz/antq "RELEASE"]
                                                 [lein-pprint "1.1.2"]
                                                 [lein-subscribable-urls "0.1.0-alpha2"]
                                                 [threatgrid/lein-lean "0.6.0"]
                                                 [jonase/eastwood "0.8.1"]
                                                 [lein-lein "0.2.0"]
                                                 [lein-jdk-tools "0.1.1"]
                                                 [threatgrid/trim-sl4j-classpath "0.2.0"]]
                      :dependencies             [[jonase/eastwood "0.8.1"]]
                      :jvm-opts                 [;; Remove useless icon from the Dock:
                                                 "-Dapple.awt.UIElement=true"
                                                 ;; Make more info available to debuggers:
                                                 "-Dclojure.compiler.disable-locals-clearing=true"
                                                 ;; If failing on startup, print stacktraces directly instead of saving them to a file:
                                                 "-Dclojure.main.report=stderr"
                                                 ;; Enable tiered compilation, for guaranteeing accurate benchmarking (at the cost of slower startup):
                                                 "-XX:+TieredCompilation"
                                                 ;; Don't elide stacktraces:
                                                 "-XX:-OmitStackTraceInFastThrow"
                                                 ;; Prevents a specific type of OOMs:
                                                 "-XX:CompressedClassSpaceSize=3G"
                                                 ;; Prevents trivial StackOverflow errors:
                                                 "-XX:MaxJavaStackTraceDepth=1000000"
                                                 ;; Set a generous limit as the maximum that can be allocated, preventing certain types of OOMs:
                                                 "-Xmx18G"
                                                 ;; increase stack size x6, for preventing SO errors:
                                                 ;;   (The current default can be found with
                                                 ;;    `java -XX:+PrintFlagsFinal -version 2>/dev/null | grep "intx ThreadStackSize"`)
                                                 "-Xss6144k"
                                                 ;; Set a minimum amount of memory that matches/exceeds a given repl's initial usage (including various tooling).
                                                 ;; This way, initial performance improves:
                                                 "-Xms6G"
                                                 ;; Improves startup time:
                                                 "-Xverify:none"
                                                 ;; Enable various optimizations, for guaranteeing accurate benchmarking (at the cost of slower startup):
                                                 "-server"]
                      :monkeypatch-clojure-test false}

 ;; Simulates the maximum heap allocation that a process can have when Xmx is left unconfigured:
 :low-mem            {:jvm-opts ["-Xmx1G"]}

 :rebel              {:dependencies [[com.bhauman/rebel-readline "0.1.4"]]}

 :reply              {:dependencies [[reply "0.3.7"]
                                     [net.cgrand/parsley "0.9.3"]]}

 ;; The following flags setup GC with short STW pauses, which tend to be apt for webserver workloads.
 ;; Taken from https://docs.oracle.com/cd/E40972_01/doc.70/e40973/cnf_jvmgc.htm#autoId2
 :g1gc               {:jvm-opts ["-XX:+UseG1GC"
                                 "-XX:MaxGCPauseMillis=200"
                                 "-XX:ParallelGCThreads=20"
                                 "-XX:ConcGCThreads=5"
                                 "-XX:InitiatingHeapOccupancyPercent=70"]}

 ;; Throws if core.async blocking ops (>!!, <!!, alts!!, alt!!) are used in a go block
 ;; (added in a separate profile since some apps break on it)
 :async-checking     {:jvm-opts ["-Dclojure.core.async.go-checking=true"]}

 ;; remember to keep this in sync with exports.sh
 :yourkit
 {:jvm-opts
  ["-agentpath:/Applications/YourKit-Java-Profiler-2019.8.app/Contents/Resources/bin/mac/libyjpagent.dylib=quiet,sessionname={YOURKIT_SESSION_NAME}"]}

 :repl               {:middleware                        [leiningen.resolve-java-sources-and-javadocs/middleware
                                                          leiningen.trim-sl4j-classpath/middleware]
                      :plugins                           [[threatgrid/resolve-java-sources-and-javadocs "1.3.0"]]
                      :resolve-java-sources-and-javadocs {:classifiers #{"sources"}}
                      :jvm-opts                          ["-Dleiningen.resolve-java-sources-and-javadocs.throw=true"]}

 :clojars            {:deploy-repositories [["clojars"
                                             {:url           "https://clojars.org/repo/",
                                              :sign-releases false}]]}

 ;; the following profile serves for two use cases:
 ;; * Launching `lein repl` from iTerm, that Emacs can eventually connect to
 ;; * Launching an in-Emacs JVM
 :emacs-backend      {:dependencies   [[clj-stacktrace "0.2.8"]
                                       [com.clojure-goes-fast/clj-java-decompiler "0.2.1"]
                                       [com.nedap.staffing-solutions/utils.collections "2.1.0"]
                                       [com.stuartsierra/component.repl "0.2.0"]
                                       [criterium "0.4.5"]
                                       [clj-kondo "2021.01.20"]
                                       [formatting-stack "4.3.0"]
                                       [org.clojure/clojurescript "1.10.764"] ;; formatting-stack transitive, removes a warning
                                       [lambdaisland/deep-diff "0.0-29"]
                                       [lambdaisland/deep-diff2 "2.0.108"]
                                       [medley "1.2.0"]
                                       [nrepl-debugger "0.1.0-SNAPSHOT" :exclusions [nrepl]]
                                       [org.clojure/clojure "1.10.1"]
                                       [org.clojure/core.incubator "0.1.4"] ;; ensure it's recent enought to avoid a warning
                                       [org.clojure/math.combinatorics "0.1.6"]
                                       [org.clojure/test.check "1.1.0"]
                                       [org.clojure/java.jmx "1.0.0"]
                                       [org.clojure/tools.trace "0.7.11"]
                                       [org.clojure/spec.alpha "0.2.194"]
                                       [org.clojure/tools.namespace "1.1.0"]
                                       ;; `lein with-profile -user,-dev do clean, pom, jar`:
                                       [org.clojure/tools.nrepl "1.100.0"] ;; 0.2.13 matches with my lib/cider/cider.el. 1.99.0 is my fork
                                       [org.clojure/tools.reader "1.3.3"]
                                       [threatgrid/formatting-stack.are-linter "0.1.0-alpha1"]
                                       ;; Ensure Jackson is consistent and up-to-date:
                                       [com.fasterxml.jackson.core/jackson-annotations "2.11.2"]
                                       [com.fasterxml.jackson.core/jackson-core "2.11.2"]
                                       [com.fasterxml.jackson.core/jackson-databind "2.11.2"]
                                       [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.11.2"]
                                       [com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.11.2"]
                                       [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.11.2"]]

                      :repositories   [["https://packagecloud.io/vemv/clojure/maven2"
                                        {:url "https://packagecloud.io/vemv/clojure/maven2"}]]

                      :source-paths   ["/Users/vemv/.lein/scripts"]

                      :jvm-opts       ["-Dformatting-stack.eastwood.parallelize-linters=true"]

                      :resource-paths [;; http://rebl.cognitect.com/download.html
                                       "/Users/vemv/.lein/resources/rebl.jar"]

                      :repl-options   {:port    41235
                                       :timeout 900000
                                       :welcome "Print nothing"
                                       :init    {:emacs-backend (clojure.core/require 'vemv.emacs-backend)}}}

 :emacs-backend-init {:repl-options {:init {:emacs-backend-init (clojure.core/require 'vemv.anyrefresh)}}}

 :iroh-global        {:dependencies      [[threatgrid/trapperkeeper "3.1.0" :exclusions [nrepl]]
                                          [io.aeron/aeron-all "1.32.0"]
                                          [threatgrid/trapperkeeper-webserver-jetty9 "4.2.0"]]
                      :target-path       "target/%s/"
                      :source-paths      [#_ "/Users/vemv/trapperkeeper-webserver-jetty9/test/clj"
                                          "/Users/vemv/formatting-stack.alias-rewriter/src"
                                          ;; `lein with-profile -user cljx once`:
                                          "/Users/vemv/schema/target/generated/src/clj"]
                      :java-source-paths [#_ "/Users/vemv/trapperkeeper-webserver-jetty9/test/java"]
                      :jvm-opts          ["-Diroh.test.dotests.elide-explanations=true"
                                          "-Diroh.dev.logging.level=:debug"
                                          "-Diroh.dev.logging.enable-println-appender=false"
                                          ;; "-Diroh.enable-response-profiling=true"
                                          "-Diroh.dev.logging.enable-file-appender=true"
                                          "-Diroh.dev.logging.order-chronologically=false"]}

 :parallel-reload    {:dependencies [[threatgrid/parallel-reload "0.3.0"]
                                     [cider/cider-nrepl "0.99.9" :exclusions [cljfmt compliment nrepl/nrepl]]
                                     [compliment "0.3.11"]
                                     [nrepl/nrepl "0.4.4"] ;; same as refactor-nrepl "2.4.0" git.io/Jt26p
                                     [refactor-nrepl "2.4.0" :exclusions [org.clojure/tools.logging
                                                                          cider-nrepl
                                                                          nrepl]]
                                     [commons-io/commons-io "2.8.0"] ;; for the Tailer class
                                     [org.clojure/clojure "1.11.99"]
                                     ;; How to create the following artifact:
                                     ;; * rename sources package to target/clojuresources-1.10.99.jar
                                     ;; * package_cloud push vemv/clojure target/clojuresources-1.10.99.jar --coordinates=org.clojuresources:clojuresources:1.10.99
                                     [org.clojuresources/clojuresources "1.10.99"]]

                      :jvm-opts     ["-Djava.awt.headless=false" ;; ensure the clipboard is usable
                                     #_ "-Dcisco.tools.namespace.parallel-refresh.debug=true"
                                     ;; experiment - try triggering GC more frequently:
                                     ;; (didn't work originally, but it might after the SoftRef hack)
                                     ;; "-XX:MaxMetaspaceExpansion=0"
                                     ]
                      :aliases      {"nrepl" ["run" "-m" "vemv.nrepl"]}}

 ;; for hacking on refactor-nrepl itself
 :refactor-nrepl     {:dependencies [[http-kit "2.3.0"]
                                     [cheshire "5.8.0"]
                                     [org.clojure/tools.analyzer.jvm "0.7.1"]
                                     [org.clojure/tools.namespace "0.3.0-alpha3"
                                      :exclusions [org.clojure/tools.reader]]
                                     [org.clojure/tools.reader "1.1.1"]
                                     [cider/orchard "0.3.0"]
                                     [cljfmt "0.6.3"]
                                     [me.raynes/fs "1.4.6"]
                                     [rewrite-clj "0.6.0"]
                                     [cljs-tooling "0.2.0"]
                                     [version-clj "0.1.2"]]}

 :emacs-figwheel     {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                     [figwheel-sidecar "0.5.16"]]
                      :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                      :figwheel     {:nrepl-middleware ["cider.nrepl/cider-middleware"
                                                        "refactor-nrepl.middleware/wrap-refactor"
                                                        "cemerick.piggieback/wrap-cljs-repl"]}}

 :eftest             {:plugins [[lein-eftest "0.5.8"]]
                      :eftest  {:multithread? false
                                :fail-fast?   true}}}
