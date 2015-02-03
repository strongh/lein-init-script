(ns leiningen.init-script
  (:use [leiningen.uberjar])
  (:require [clojure.java.io :as io]))


(defn format-properties [opts]
  (if (nil? (:properties opts))
    ""
    (apply str (interpose " " (map #(str "-D" (name %) "=\""
                                         (% (:properties opts)) "\"")
                                   (keys (:properties opts)))))))

(defn format-opts [opts k]
  (let [opts (get opts k)]
    (apply str (interpose " " opts))))

(defn format-java-string [opts]
  (str (format-properties opts) " " (format-opts opts :jvm-opts)))

(defn resource-input-stream [res-name]
  (.getResourceAsStream (.getContextClassLoader (Thread/currentThread)) res-name))

(def init-script-template (slurp (resource-input-stream "init-script-template")))
(def install-template (slurp (resource-input-stream "install-template")))
(def clean-template (slurp (resource-input-stream "clean-template")))

(defn gen-init-script [project opts]
  (let [name (or (:artifact-name opts) (:name project))
        version (:version project)
        description (:description project)
        pid-dir (:pid-dir opts)
        jar-install-dir (:jar-install-dir opts)
        java-flags (format-java-string opts)
        jar-args (format-opts opts :jar-args)
        redirect-output-to (:redirect-output-to opts)]
    (format init-script-template
            name
            version
            pid-dir
            jar-install-dir
            java-flags
            jar-args
            redirect-output-to)))

(defn gen-install-script [uberjar-path init-script-path opts]
  (let [jar-install-dir (:jar-install-dir opts)
        init-script-install-dir (:init-script-install-dir opts)
        name (or (:artifact-name opts) (:name opts))
        version (:version opts)
        installed-init-script-path (str init-script-install-dir "/" name "d")]
    (format install-template
            name
            version
            jar-install-dir
            uberjar-path
            init-script-install-dir
            init-script-path
            installed-init-script-path)))

(defn gen-clean-script [project opts]
  (let [jar-install-dir (:jar-install-dir opts)
        init-script-install-dir (:init-script-install-dir opts)
        name (or (:artifact-name opts) (:name project))]
    (format clean-template name jar-install-dir init-script-install-dir)))

(defn create-output-dir [path]
  (.mkdirs (java.io.File. path)))

(defn create-script [path content]
  (do (spit path content)
      (doto
          (java.io.File. path)
        (.setExecutable true))))

(defn defaults [project]
  (let [name (:name project)
        root (:root project)
        version (:version project)]
    {:name name
     :pid-dir "/var/run"
     :jar-install-dir (str "/usr/local/" name)
     :init-script-install-dir "/etc/init.d"
     :artifact-dir (str root "/init-script")
     :redirect-output-to "/dev/null"
     :run-uberjar? true
     :version version}))

(defn- get-source-uberjar-path
  "The path of the uberjar that is written under the 'target' dir is subject
  to change depending on the version of of leiningen in use. The three paths below
  are what I've seen thus far -- look in each location for the uberjar until found."
  [{:keys [target-path root] :as project} {:keys [name version] :as opts}]
  (let [paths [(str target-path "/" (:name opts) "-" version "-standalone.jar")
               (str target-path "+uberjar/" (:name opts) "-" version "-standalone.jar")
               (str root "/target/" (:name opts) "-" version "-standalone.jar")]]
    (loop [p (first paths)
           paths (rest paths)]
      (if (or (.exists (io/as-file p)) (empty? paths))
        p
        (recur (first paths) (rest paths))))))

(defn init-script
  "A leiningen plugin that allows you to generate *NIX init scripts."
  [project]
  (let [opts (merge (defaults project) (:lis-opts project))
        name (or (:artifact-name opts) (:name opts))
        version (:version opts)
        artifact-dir (:artifact-dir opts) ;; the init-script dir
        run-uberjar? (:run-uberjar? opts)
        source-uberjar-path (get-source-uberjar-path project opts)
        artifact-uberjar-path (format "%s/%s-%s-standalone.jar" artifact-dir name version)
        artifact-init-script-path (str artifact-dir "/" name "d")
        install-script-path (str artifact-dir "/" "install-" name)
        clean-script-path (str artifact-dir "/" "clean-" name)]
    (create-output-dir artifact-dir) ;; Creates directory for init-scripts
    (when run-uberjar?
      (uberjar project)) ;; Leiningen task that creates uberjars

    ;; Copy from source dir (where uberjars have been created by leiningen) to artifact-path
    (io/copy (java.io.File. source-uberjar-path) (java.io.File. artifact-uberjar-path))
    (create-script
     artifact-init-script-path (gen-init-script project opts))
    (create-script
     install-script-path
     (gen-install-script artifact-uberjar-path artifact-init-script-path opts))
    (create-script
     clean-script-path (gen-clean-script project opts))
    (println (str "*** Done generating init scripts, see the " artifact-dir " directory"))))
