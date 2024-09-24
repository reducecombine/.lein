{:user               {:plugins                  [[lein-collisions "0.1.4"]
                                                 [lein-pprint "1.1.2"]
                                                 [lein-subscribable-urls "0.1.0-alpha2"]
                                                 [threatgrid/lein-lean "0.6.0"]
                                                 [lein-lein "0.2.0"]
                                                 [jonase/eastwood "1.4.2"]
                                                 [lein-cloverage "1.2.3"]
                                                 [com.github.clj-kondo/lein-clj-kondo "2024.03.13"]]
                      :dependencies             [[jonase/eastwood "1.4.2"]]
                      :jvm-opts                 [;; Disable all UI features for disabling the clipboard - for personal security:
                                                 "-Djava.awt.headless=true"
                                                 "-Dmush.enable-tap-logger=true"
                                                 ;; Remove useless icon from the Dock:
                                                 "-Dapple.awt.UIElement=true"
                                                 ;; Make more info available to debuggers:
                                                 "-Dclojure.compiler.disable-locals-clearing=true"
                                                 ;; If failing on startup, print stacktraces directly instead of saving them to a file:
                                                 "-Dclojure.main.report=stderr"
                                                 ;; Changes nothing - just to remember how it's done:
                                                 "-Dclojure.spec.skip-macros=false"
                                                 "-Dclojure.spec.compile-asserts=true"
                                                 "-Dclojure.spec.check-asserts=true"
                                                 ;; Changes nothing - just to remember how it's done:
                                                 "-Drefactor-nrepl.internal.pst=true"
                                                 "-Dunep.gpml.skip-reset-on-startup=true"
                                                 ;; For very occasional debugging:
                                                 #_"-Djavax.net.debug=all"
                                                 #_"-Djavax.net.debug=ssl"
                                                 #_"-Djavax.net.debug=help"
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
                                                 ;; Enable various optimizations, for guaranteeing accurate benchmarking (at the cost of slower startup):
                                                 "-server"]
                      :monkeypatch-clojure-test false}

 :kondo-lint {:clj-kondo {:config {:lint-as {garden.selectors/defpseudoclass clojure.core/defn
                                             garden.selectors/defselector clj-kondo.lint-as/def-catch-all}
                                   :linters {:unresolved-var                        {:exclude [ring.util.http-response/content-type]}
                                             :docstring-leading-trailing-whitespace {:level :warning}
                                             :keyword-binding                       {:level :warning}
                                             :reduce-without-init                   {:level :warning}
                                             :redundant-fn-wrapper                  {:level :warning}
                                             :single-key-in                         {:level :warning}
                                             :unsorted-required-namespaces          {:level :warning}
                                             :used-underscored-binding              {:level :warning}}}}}

 :antq {:plugins [[com.github.liquidz/antq "1.3.2"]]}

 ;; Simulates the maximum heap allocation that a process can have when Xmx is left unconfigured:
 :low-mem            {:jvm-opts ["-Xmx1G"]}

 :nvd                {:dependencies [[nvd-clojure "2.7.0"]]}

 :rebel              {:dependencies [[com.bhauman/rebel-readline "0.1.4" :exclusions [cljfmt]]]}

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

 :yourkit
 {:jvm-opts
  ;; quiet - disable logging to stdout
  ;; probe_on=com.yourkit.probes.builtin.Databases - enables the jdbc probe globally, for registering SQL query events
  ;; sessionname={YOURKIT_SESSION_NAME} - gives the YourKit process a name,
  ;;   based on `export YOURKIT_SESSION_NAME="$(basename $PWD)"`, which I do from my repl script
  ["-agentpath:/Applications/YourKit-Java-Profiler-2023.9.app/Contents/Resources/bin/mac/libyjpagent.dylib=quiet,probe_on=com.yourkit.probes.builtin.Databases,sessionname=${YOURKIT_SESSION_NAME}"]}

 #_ #_
 :repl               {:middleware                        [cider.enrich-classpath.plugin-v2/middleware]
                      :plugins                           [[mx.cider/lein-enrich-classpath "1.15.0"]]
                      :enrich-classpath                  {:shorten true}
                      :jvm-opts                          ["-Dcider.enrich-classpath.throw=true"
                                                          ;; the following opt appears to break refactor when using upstream
                                                          "-Drefactor-nrepl.internal.try-requiring-tools-nrepl=true"]}

 :clojars            {:deploy-repositories [["clojars"
                                             {:url           "https://clojars.org/repo/",
                                              :sign-releases false}]]}

 ;; the following profile serves for two use cases:
 ;; * Launching `lein repl` from iTerm, that Emacs can eventually connect to
 ;; * Launching an in-Emacs JVM
 ;; I don't directly use some of these libraries, but they are here (set to their latest version)
 ;; for avoiding hitting `:pedantic?` warns in projects that set that option unconditionally.
 :emacs-backend      {:dependencies   [[clj-stacktrace "0.2.8"]
                                       [com.clojure-goes-fast/clj-java-decompiler "0.3.4"]
                                       [com.nedap.staffing-solutions/utils.collections "2.1.0"]
                                       [org.clojure/tools.deps "0.18.1354"]
                                       [com.stuartsierra/component.repl "0.2.0"]
                                       [spec-coerce "1.0.0-alpha16"]
                                       ;; disabled b/c Mush Rama etc
                                       #_ [com.stuartsierra/component "1.0.0"]
                                       [com.stuartsierra/dependency "1.0.0"]
                                       [prismatic/schema "1.1.9"]
                                       [com.taoensso/tufte "2.1.0"]
                                       [com.nedap.staffing-solutions/utils.collections "2.2.0"]
                                       [criterium "0.4.5"]
                                       [com.gfredericks/test.chuck "0.2.13"]

                                       ;; Bump these in concert:
                                       [clj-kondo "2024.03.13"]
                                       [borkdude/edamame "1.4.25"]
                                       [babashka/fs "0.5.20"]

                                       [nubank/matcher-combinators "3.8.8"]
                                       [formatting-stack "4.6.0" :exclusions [cljfmt]]
                                       [dev.weavejester/cljfmt "0.12.0"]
                                       #_ [net.vemv/with "0.1.0"]
                                       [frak "0.1.9"]

                                       [org.clojure/clojurescript "1.11.60"] ;; formatting-stack transitive, removes a warning
                                       [com.google.javascript/closure-compiler-unshaded "v20220502"] ;; please ensure it mirrors cljs's choice
                                       [com.google.guava/guava "31.1-jre"]

                                       [com.github.seancorfield/honeysql "2.5.1103"]
                                       [com.github.seancorfield/next.jdbc "1.3.909"]

                                       [lambdaisland/deep-diff "0.0-29"]
                                       [lambdaisland/deep-diff2 "2.0.108"]
                                       [dev.weavejester/medley "1.6.0"]
                                       [mvxcvi/puget "1.1.1"]
                                       [fipp "0.6.25"]
                                       [fmnoise/flow "4.1.0"]
                                       [io.aviso/pretty "1.1.1"]
                                       [mvxcvi/arrangement "1.2.1"]
                                       [nrepl-debugger "0.1.0-SNAPSHOT" :exclusions [nrepl]]
                                       [org.clojure/core.async "1.5.648"]
                                       [org.clojure/core.cache "1.0.207"]
                                       [org.clojure/core.incubator "0.1.4"] ;; ensure it's recent enought to avoid a warning
                                       [org.clojure/core.memoize "1.0.236"]
                                       [org.clojure/core.rrb-vector "0.1.1"]
                                       [org.clojure/data.csv "1.0.0"]
                                       [org.clojure/data.json "2.0.1"]
                                       [org.clojure/data.priority-map "1.0.0"]
                                       [org.clojure/data.zip "1.0.0"]
                                       [org.clojure/math.combinatorics "0.1.6"]
                                       [org.clojure/test.check "1.1.1"]
                                       [org.clojure/java.jmx "1.0.0"]
                                       [org.clojure/tools.analyzer.jvm "1.2.2"]
                                       [org.clojure/tools.logging "1.2.4"]
                                       [org.clojure/tools.trace "0.7.11"]
                                       [org.clojure/spec.alpha "0.2.194"]
                                       [org.clojure/tools.namespace "1.4.4"]
                                       ;; `lein with-profile -user,-dev do clean, install; lein with-profile -user,-dev do clean, pom, jar, clean, install; `:
                                       #_ [org.clojure/tools.nrepl "1.100.0"] ;; 0.2.13 matches with my lib/cider/cider.el. 1.100.0 is my fork
                                       [org.clojure/tools.reader "1.3.3"]
                                       [rewrite-clj "1.1.47"]
                                       [threatgrid/formatting-stack.are-linter "0.1.0-alpha1"]
                                       [zprint "1.2.3"]

                                       ;; How to create the following artifact:
                                       ;; mvn clean package clean install
                                       ;; package_cloud push vemv/clojure target/clojure-1.11.900.jar
                                       #_ [org.clojure/clojure "1.12.900"]
                                       [threatgrid/parallel-reload "0.4.1" :exclusions [org.clojure/clojure]]
                                       [commons-io/commons-io "2.15.0"] ;; for the Tailer class

                                       ;; Ensure Jackson is consistent and up-to-date:
                                       [com.fasterxml.jackson.core/jackson-annotations "2.15.2"]
                                       [com.fasterxml.jackson.core/jackson-core "2.15.2"]
                                       [com.fasterxml.jackson.core/jackson-databind "2.15.2"]
                                       [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.15.2"]
                                       [com.fasterxml.jackson.datatype/jackson-datatype-jsr310 "2.15.2"]
                                       [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.15.2"]]

                      :repositories   [["https://packagecloud.io/vemv/clojure/maven2"
                                        {:url "https://packagecloud.io/vemv/clojure/maven2"}]
                                       ["https://packagecloud.io/vemv/cider/maven2"
                                        {:url "https://packagecloud.io/vemv/cider/maven2"}]]

                      :source-paths   ["/Users/vemv/.lein/scripts"
                                       "/Users/vemv/formatting-stack/src"
                                       "/Users/vemv/formatting-stack/worker"
                                       "/Users/vemv/formatting-stack.alias-rewriter/src"
                                       "/Users/vemv/parallel-reload/src"
                                       "/Users/vemv/eastwood/src"
                                       "/Users/vemv/eastwood/copied-deps"
                                       "/Users/vemv/tufte.auto/src"]

                      :jvm-opts       ["-Dformatting-stack.eastwood.parallelize-linters=true"]

                      :resource-paths ["/Users/vemv/formatting-stack/resources"
                                       "/Users/vemv/eastwood/resource"
                                       "/Users/vemv/eastwood/resources"
                                       ;; http://rebl.cognitect.com/download.html
                                       "/Users/vemv/.lein/resources/rebl.jar"]

                      :repl-options   { ;; :port    41235
                                       :timeout 900000
                                       :welcome "Print nothing"
                                       :init    {:emacs-backend (clojure.core/require 'vemv.emacs-backend)}}}

 :emacs-backend-init {:dependencies [[cider/orchard "0.25.0"]
                                     [mx.cider/haystack "RELEASE" :exclusions [cider/orchard]]]
                      :repl-options {:init {:emacs-backend-init (clojure.core/require 'vemv.anyrefresh)}}}

 :parallel-reload    {:dependencies [

                                     ;; How to create the following artifact:
                                     ;; ~/cider-nrepl at `vemv` branch
                                     ;; ./build.sh
                                     ;; package_cloud push vemv/cider target/cider-nrepl-0.99.10.jar
                                     [cider/cider-nrepl "0.99.10" :exclusions [nrepl/nrepl]]
                                     [nrepl/nrepl "0.4.4"] ;; same as refactor-nrepl "2.4.0" git.io/Jt26p
                                     ]

                      :jvm-opts     [#_ "-Dcisco.tools.namespace.parallel-refresh.debug=true"
                                     ;; experiment - try triggering GC more frequently:
                                     ;; (didn't work originally, but it might after the SoftRef hack)
                                     ;; "-XX:MaxMetaspaceExpansion=0"
                                     ]
                      :aliases      {"nrepl" ["run" "-m" "vemv.nrepl"]}}

 ;; NOTE: bump these if ever seeing `Unable to resolve var: cider.nrepl/wrap-log in this context` on startup
 :cider-nrepl-latest {:dependencies [#_[cider/cider-nrepl "0.36.1" :exclusions [nrepl/nrepl]]
                                     [nrepl/nrepl "1.0.0"]]
                      :plugins [[cider/cider-nrepl "0.48.0" :exclusions [nrepl/nrepl]]
                                [refactor-nrepl "3.9.1" :exclusions [org.clojure/tools.logging
                                                                     cider-nrepl
                                                                     nrepl]]]}


 #_ #_
 :emacs-figwheel     {:dependencies [[com.cemerick/piggieback "0.5.3"]
                                     [figwheel-sidecar "0.5.16"]]
                      :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                      :figwheel     {:nrepl-middleware ["cider.nrepl/cider-middleware"
                                                        "refactor-nrepl.middleware/wrap-refactor"
                                                        "cemerick.piggieback/wrap-cljs-repl"]}}

 :eastwood-ci-clojure-1-10 {:dependencies [[org.clojure/clojure "1.10.3"]]}

 :eastwood-ci-clojure-1-11 {:dependencies [[org.clojure/clojure "1.11.0-alpha3"]
                                           [org.clojure/spec.alpha "0.3.214"]]}}
