(ns heraldicon.frontend.search-string
  (:require
   [clojure.string :as str]
   [heraldicon.util.cache :as cache]))

(defn- normalize-string [s]
  (some-> s
          (.normalize "NFD")))

(defonce normalize-string-for-match-cache
  (cache/lru-cache 100000))

(defn strip-surrounding-quotes [s]
  (if (and (string? s)
           (>= (count s) 2)
           (let [first-char (first s)
                 last-char (last s)]
             (and (= first-char \") (= last-char \"))))
    (subs s 1 (dec (count s)))
    s))

(defn normalize-string-for-match [s]
  (let [value (cache/get normalize-string-for-match-cache s)]
    (if (some? value)
      value
      (let [value (some-> s
                          normalize-string
                          (str/replace #"[\u0300-\u036f]" "")
                          str/lower-case)]
        (cache/put normalize-string-for-match-cache s value)
        value))))

(defonce matches-word-cache
  (cache/lru-cache 1000000))

(defn escape-regex [s]
  (let [special-chars (set "\\^$.|?*+()[]{}")]
    (->> s
         (map #(if (special-chars %)
                 (str "\\" %)
                 %))
         (apply str))))

(defonce string-matches?-cache
  (cache/lru-cache 1000000))

(defn- string-matches?
  [s word]
  (let [key [s word]
        value (cache/get string-matches?-cache key)]
    (if (some? value)
      value
      (let [value (cond
                    (and (= (first word) "/")
                         (= (last word) "/")) (try
                                                (re-find (re-pattern (subs word 1 (dec (count word)))) s)
                                                (catch :default _
                                                  nil))

                    (and (= (first word) "\"")
                         (= (last word) "\"")) (let [bounded-regex (re-pattern (str "\\b" (escape-regex (subs word 1 (dec (count word)))) "\\b"))]
                                                 (re-find bounded-regex s))

                    :else (str/includes? s (str/replace word "\"" "")))]
        (cache/put string-matches?-cache key (boolean value))
        value))))

(defn matches-word? [data word]
  (let [key [data word]
        value (cache/get matches-word-cache key)]
    (if (some? value)
      value
      (let [value (cond
                    (keyword? data) (-> data name (matches-word? word))
                    (string? data) (-> data
                                       normalize-string-for-match
                                       (string-matches? word))
                    (vector? data) (some (fn [e]
                                           (matches-word? e word)) data)
                    (map? data) (some (fn [[k v]]
                                        (or (and (keyword? k)
                                                 (matches-word? k word)
                                                  ;; this would be an attribute entry, the value
                                                  ;; must be truthy as well
                                                 v)
                                            (matches-word? v word))) data))]
        (cache/put matches-word-cache key (boolean value))
        value))))

(defonce split-search-string-cache
  (cache/lru-cache 100000))

(defn split
  [s]
  (let [value (cache/get split-search-string-cache s)]
    (if (some? value)
      value
      (let [matches (re-seq #"/[^/]*/|\"[^\"]+\"|\S+" (normalize-string-for-match s))
            value (->> matches
                       (map strip-surrounding-quotes)
                       (filterv (fn [s]
                                  (pos? (count s)))))]
        (cache/put split-search-string-cache s value)
        value))))
