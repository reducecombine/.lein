{:user               {:plugins                  [[com.github.liquidz/antq "RELEASE"]
                                                 [lein-collisions "0.1.4"]
                                                 [lein-pprint "1.1.2"]
                                                 [lein-subscribable-urls "0.1.0-alpha2"]
                                                 [threatgrid/lein-lean "0.6.0"]
                                                 [jonase/eastwood "0.9.6"]
                                                 [lein-lein "0.2.0"]
                                                 [lein-jdk-tools "0.1.1"]]
                      :dependencies             [[jonase/eastwood "0.9.6"]]
                      :jvm-opts                 [;; Disable all UI features for disabling the clipboard - for personal security:
                                                 "-Djava.awt.headless=true"
                                                 ;; Remove useless icon from the Dock:
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
                                                 "-Xmx24G"
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

 :nvd                {:dependencies [[lein-nvd "1.4.1"]]}

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

 :repl               {:middleware                        [cider.enrich-classpath/middleware]
                      :plugins                           [[mx.cider/enrich-classpath "1.4.0-alpha2"]]
                      :resolve-java-sources-and-javadocs {:classifiers #{"sources"}}
                      :enrich-classpath                  {:classifiers #{"sources"}}
                      :jvm-opts                          ["-Dcider.enrich-classpath.throw=true"]}

 :clojars            {:deploy-repositories [["clojars"
                                             {:url           "https://clojars.org/repo/",
                                              :sign-releases false}]]}

 ;; the following profile serves for two use cases:
 ;; * Launching `lein repl` from iTerm, that Emacs can eventually connect to
 ;; * Launching an in-Emacs JVM
 ;; I don't directly use some of these libraries, but they are here (set to their latest version)
 ;; for avoiding hitting `:pedantic?` warns in projects that set that option unconditionally.
 :emacs-backend      {:dependencies   [[clj-stacktrace "0.2.8"]
                                       [com.clojure-goes-fast/clj-java-decompiler "0.2.1"]
                                       [com.nedap.staffing-solutions/utils.collections "2.1.0"]
                                       [com.stuartsierra/component.repl "0.2.0"]
                                       [com.stuartsierra/component "1.0.0"]
                                       [com.stuartsierra/dependency "1.0.0"]
                                       [com.nedap.staffing-solutions/utils.collections "2.2.0"]
                                       [criterium "0.4.5"]
                                       [com.gfredericks/lein-how-to-ns "0.2.9"]
                                       [clj-kondo "2021.06.18"]
                                       [formatting-stack "4.3.0"]
                                       [org.clojure/clojurescript "1.10.764"] ;; formatting-stack transitive, removes a warning
                                       [lambdaisland/deep-diff "0.0-29"]
                                       [lambdaisland/deep-diff2 "2.0.108"]
                                       [medley "1.2.0"]
                                       [mvxcvi/puget "1.1.1"]
                                       [fipp "0.6.23"]
                                       [com.google.guava/guava "25.1-jre"]
                                       [mvxcvi/arrangement "1.2.1"]
                                       [nrepl-debugger "0.1.0-SNAPSHOT" :exclusions [nrepl]]
                                       [org.clojure/clojure "1.10.2"]
                                       [org.clojure/core.async "1.3.610"]
                                       [org.clojure/core.cache "1.0.207"]
                                       [org.clojure/core.incubator "0.1.4"] ;; ensure it's recent enought to avoid a warning
                                       [org.clojure/core.memoize "1.0.236"]
                                       [org.clojure/core.rrb-vector "0.1.1"]
                                       [org.clojure/data.csv "1.0.0"]
                                       [org.clojure/data.json "2.0.1"]
                                       [org.clojure/data.priority-map "1.0.0"]
                                       [org.clojure/data.zip "1.0.0"]
                                       [org.clojure/math.combinatorics "0.1.6"]
                                       [org.clojure/test.check "1.1.0"]
                                       [org.clojure/java.jmx "1.0.0"]
                                       [org.clojure/tools.trace "0.7.11"]
                                       [org.clojure/spec.alpha "0.2.194"]
                                       [org.clojure/tools.namespace "1.1.0"]
                                       ;; `lein with-profile -user,-dev do clean, pom, jar`:
                                       [org.clojure/tools.nrepl "1.100.0"] ;; 0.2.13 matches with my lib/cider/cider.el. 1.99.0 is my fork
                                       [org.clojure/tools.reader "1.3.3"]
                                       [rewrite-clj "1.0.594-alpha"]
                                       [threatgrid/formatting-stack.are-linter "0.1.0-alpha1"]
                                       ;; Ensure Jackson is consistent and up-to-date:
                                       [com.fasterxml.jackson.core/jackson-annotations "2.12.3"]
                                       [com.fasterxml.jackson.core/jackson-core "2.12.3"]
                                       [com.fasterxml.jackson.core/jackson-databind "2.12.3"]
                                       [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.12.3"]
                                       [com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.12.3"]
                                       [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.12.3"]]

                      :repositories   [["https://packagecloud.io/vemv/clojure/maven2"
                                        {:url "https://packagecloud.io/vemv/clojure/maven2"}]
                                       ["https://packagecloud.io/vemv/cider/maven2"
                                        {:url "https://packagecloud.io/vemv/cider/maven2"}]]

                      :source-paths   ["/Users/vemv/.lein/scripts"
                                       "/Users/vemv/formatting-stack.alias-rewriter/src"]

                      :jvm-opts       ["-Dformatting-stack.eastwood.parallelize-linters=true"]

                      :resource-paths [;; http://rebl.cognitect.com/download.html
                                       "/Users/vemv/.lein/resources/rebl.jar"]

                      :repl-options   {:port    41235
                                       :timeout 900000
                                       :welcome "Print nothing"
                                       :init    {:emacs-backend (clojure.core/require 'vemv.emacs-backend)}}}

 :emacs-backend-init {:repl-options {:init {:emacs-backend-init (clojure.core/require 'vemv.anyrefresh)}}}

 :parallel-reload    {:dependencies [[threatgrid/parallel-reload "0.4.1" :exclusions [org.clojure/clojure]]

                                     ;; How to create the following artifact:
                                     ;; ~/cider-nrepl at `vemv` branch
                                     ;; ./build.sh
                                     ;; package_cloud push vemv/cider target/cider-nrepl-0.99.9.jar
                                     [cider/cider-nrepl "0.99.9" :exclusions [cljfmt compliment nrepl/nrepl]]
                                     [compliment "0.3.11"]
                                     [nrepl/nrepl "0.4.4"] ;; same as refactor-nrepl "2.4.0" git.io/Jt26p
                                     [refactor-nrepl "2.4.0" :exclusions [org.clojure/tools.logging
                                                                          cider-nrepl
                                                                          nrepl]]
                                     [commons-io/commons-io "2.8.0"] ;; for the Tailer class
                                     ;; How to create the following artifact:
                                     ;; mvn clean package
                                     ;; package_cloud push vemv/clojure target/clojure-1.10.100.jar
                                     [org.clojure/clojure "1.10.100"] ;; version 1.11 with weakref patch applied. (pretends to be 1.10 b/c of a kondo thing)
                                     ;; How to create the following artifact:
                                     ;; * rename sources package in target/ to clojuresources-1.10.100.jar
                                     ;; * package_cloud push vemv/clojure target/clojuresources-1.10.100.jar --coordinates=org.clojuresources:clojuresources:1.10.100
                                     [org.clojuresources/clojuresources "1.10.100"]]

                      :jvm-opts     ["-Djava.awt.headless=false" ;; ensure the clipboard is usable
                                     #_ "-Dcisco.tools.namespace.parallel-refresh.debug=true"
                                     ;; experiment - try triggering GC more frequently:
                                     ;; (didn't work originally, but it might after the SoftRef hack)
                                     ;; "-XX:MaxMetaspaceExpansion=0"
                                     ]
                      :aliases      {"nrepl" ["run" "-m" "vemv.nrepl"]}}

 :emacs-figwheel     {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                     [figwheel-sidecar "0.5.16"]]
                      :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                      :figwheel     {:nrepl-middleware ["cider.nrepl/cider-middleware"
                                                        "refactor-nrepl.middleware/wrap-refactor"
                                                        "cemerick.piggieback/wrap-cljs-repl"]}}

 :eftest             {:plugins [[lein-eftest "0.5.8"]]
                      :eftest  {:multithread? false
                                :fail-fast?   true}}}
