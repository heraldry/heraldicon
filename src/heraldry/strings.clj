(ns heraldry.strings
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [shadow.build.warnings :as warnings]
   [shadow.resource :as res]
   [taoensso.timbre :as log]))

(def required-key-pattern
  #"^[a-z0-9.?-]+$")

(defn build-keyword-keys [data & {:keys [prefix
                                         source-file]}]
  (->> data
       (mapcat (fn [[k v]]
                 (if-not (re-matches required-key-pattern k)
                   (do
                     (warnings/print-warning {:msg (str "Warning: key '" k "' doesn't match pattern: " required-key-pattern)
                                              :line 1
                                              :column 1
                                              :file source-file
                                              :soure-name source-file
                                              :resource-name source-file
                                              :source-excerpt {:start-idx 0
                                                               :before ["<unknown>"]
                                                               :after ["<unknown>"]
                                                               :line (str "\"" k "\"")}})
                     [])
                   (if (map? v)
                     (build-keyword-keys v
                                         :prefix (str prefix "." k)
                                         :source-file source-file)
                     [[(keyword prefix k) v]]))))
       (into {})))

(defmacro load-strings [filename]
  (let [data (json/read-str (res/slurp-resource &env filename))]
    (build-keyword-keys data
                        :prefix "string"
                        :source-file filename)))

(defn check-translation-string-usage []
  (let [json-data (load-strings "en-UK.json")
        files (into []
                    (filter #(re-matches #".*\.cljs" (.getName %)))
                    (file-seq (io/file "src")))]
    (doall
     (for [file files]
       (->> file
            .getAbsolutePath
            slurp
            (re-seq #":(string[.][a-z0-9.?-]+/[a-z0-9.?-]+)")
            (map (fn [[_ s]]
                   (let [k (keyword s)]
                     (when-not (contains? json-data k)
                       (log/warn (str "Unknown key '" k "'"))))))
            doall)))
    nil))

(comment
  (check-translation-string-usage)

  ;;
  )
