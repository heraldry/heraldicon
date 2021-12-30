(ns heraldry.gettext
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as s]
   [pottery.core :as pottery]
   [pottery.scan :as pottery-scan]
   [shadow.build.warnings :as warnings]
   [shadow.resource :as res]))

(defmacro inline-dict [filename]
  (pottery/read-po-str (res/slurp-resource &env filename)))

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

(defmacro inline-dict-json [filename]
  (let [data (json/read-str (res/slurp-resource &env filename))]
    (build-keyword-keys data
                        :prefix "string"
                        :source-file filename)))

(defn gettext-do-scan! []
  (pottery/scan-codebase!
   {:dir "src"
    :template-file (io/file "gettext/template.po")
    :extract-fn (pottery/make-extractor
                 ['string (s :guard string?)] s
                 [(:or 'string) & _] (pottery-scan/extraction-warning
                                      "Could not extract strings for the form:"))}))

(defn replace-string [keyword-lookup [all s]]
  (let [k (get keyword-lookup s)]
    (if k
      (str k)
      all)))

(defn replace-strings-by-keywords []
  (let [json-data (inline-dict-json "en-UK.json")
        keyword-lookup (->> json-data
                            (group-by second)
                            (map (fn [[k v]]
                                   (when (-> v count (> 1))
                                     (println "warning: best ambiguous keyword: " (ffirst v)))
                                   [k (ffirst v)]))
                            (into {}))
        replace-fn (fn [args]
                     (replace-string keyword-lookup args))
        files (into []
                    (filter #(re-matches #".*\.cljs" (.getName %)))
                    (file-seq (io/file "src")))]
    (doall
     (for [file files]
       (let [file-path (.getAbsolutePath file)
             data (slurp file-path)]
         (->> (s/replace data #"\(string \"([^\"]*)\"\)" replace-fn)
              (spit file-path)))))
    nil))

(defn check-translation-string-usage []
  (let [json-data (inline-dict-json "en-UK.json")
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
                       (println (str "Warning: unknown key '" k "'"))))))
            doall)))
    nil))

(comment
  (gettext-do-scan!)

  (replace-strings-by-keywords)

  (check-translation-string-usage)

  ;;
  )
