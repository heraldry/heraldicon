(ns heraldicon.localization.locale
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [shadow.build.warnings :as warnings]
   [shadow.resource :as res]
   [taoensso.timbre :as log]))

(def ^:private required-key-pattern
  #"^[a-z0-9.?-]+$")

(defn- build-keyword-keys [data & {:keys [prefix
                                          source-file]}]
  (into {}
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
        data))

(defmacro ^:private load-locale [filename]
  (let [data (json/read-str (res/slurp-resource &env filename))]
    (build-keyword-keys data
                        :prefix "string"
                        :source-file filename)))

(def ^:private all-languages
  {:en (load-locale "en/strings.json")
   :de (load-locale "de/strings.json")
   :es (load-locale "es/strings.json")
   :fr (load-locale "fr/strings.json")
   :it (load-locale "it/strings.json")
   :pt (load-locale "pt-PT/strings.json")
   :ru (load-locale "ru/strings.json")
   :uk (load-locale "uk/strings.json")})

(defn- check-translation-string-usage []
  (let [json-data (apply merge (vals all-languages))
        files (into []
                    (filter #(re-matches #".*\.cljs" (.getName %)))
                    (file-seq (io/file "src")))
        used-keywords (into #{}
                            (apply concat
                                   (for [file files]
                                     (->> file
                                          .getAbsolutePath
                                          slurp
                                          (re-seq #":(string[.][a-z0-9.?-]+)/([a-z0-9.?-]+)")
                                          (map (fn [[_ namespace key]]
                                                 (keyword namespace key)))))))]
    (doseq [k used-keywords]
      (when-not (contains? json-data k)
        (log/warn (str "Unknown string key '" k "'"))))
    (doseq [k (keys json-data)]
      (when-not (contains? used-keywords k)
        (log/warn (str "Unused string key '" k "'"))))
    nil))

(comment
  (check-translation-string-usage)

  ;;
  )
