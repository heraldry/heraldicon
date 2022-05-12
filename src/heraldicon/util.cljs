(ns heraldicon.util
  (:require
   [clojure.string :as s]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.config :as config]))

(defn deep-merge-with [f & maps]
  (apply
   (fn m [& maps]
     (if (every? map? maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))

(defn percent-of [base-value]
  (fn [v]
    (when v
      (-> v
          (* base-value)
          (/ 100)))))

(defn xor [a b]
  (or (and a (not b))
      (and (not a) b)))

(defn keyword->str [k]
  (-> k
      str
      (subs 1)))

(defn upper-case-first-str [s]
  (str (s/upper-case (or (first s) "")) (s/join (rest s))))

(defn upper-case-first [s]
  (if (map? s)
    (->> s
         (map (fn [[k v]]
                [k (upper-case-first-str v)]))
         (into {}))
    (upper-case-first-str s)))

(defn translate [keyword]
  (when keyword
    (-> keyword
        name
        (s/replace "-" " ")
        (s/replace "fleur de lis" "fleur-de-lis")
        (s/replace "fleur de lys" "fleur-de-lys"))))

(defn translate-line [{:keys [type]}]
  (when (not= type :straight)
    (translate type)))

(defn translate-cap-first [keyword]
  (-> keyword
      translate
      upper-case-first))

(defn normalize-string [s]
  (some-> s
          (.normalize "NFD")))

(defn normalize-string-for-sort [s]
  (some-> s
          normalize-string
          s/lower-case))

(defn normalize-string-for-match [s]
  (some-> s
          normalize-string
          (s/replace #"[\u0300-\u036f]" "")
          s/lower-case))

(defn matches-word [data word]
  (cond
    (keyword? data) (-> data name (matches-word word))
    (string? data) (-> data normalize-string-for-match
                       (s/includes? word))
    (vector? data) (some (fn [e]
                           (matches-word e word)) data)
    (map? data) (some (fn [[k v]]
                        (or (and (keyword? k)
                                 (matches-word k word)
                                 ;; this would be an attribute entry, the value
                                 ;; must be truthy as well
                                 v)
                            (matches-word v word))) data)))

(defn index-of [item coll]
  (count (take-while (partial not= item) coll)))

(defn sanitize-string [data]
  (-> data
      (or "")
      (s/replace #"  *" " ")
      s/trim))

(defn sanitize-keyword [data]
  (-> (if (keyword? data)
        (name data)
        data)
      sanitize-string
      s/lower-case
      (s/replace #"[^a-z-]" "-")
      (s/replace #"^--*" "")
      (s/replace #"--*$" "")
      keyword))

(defn wait-for-all [chans]
  (go-catch
   (loop [result []
          [c & rest] chans]
     (if c
       (let [arms (<? c)]
         (recur (conj result arms) rest))
       result))))

(defn avatar-url [username]
  (str (or (config/get :heraldicon-site-url)
           (config/get :heraldicon-url))
       "/avatar/" username))

(defn integer-string? [s]
  (re-matches #"^[0-9]+$" s))
