(ns heraldicon.util
  (:require
   [clojure.string :as s]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.config :as config]
   [heraldicon.localization.string :as string]))

(defn deep-merge-with [f & maps]
  (apply
   (fn m [& maps]
     (if (every? map? maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))

(defn xor [a b]
  (or (and a (not b))
      (and (not a) b)))

(defn keyword->str [k]
  (-> k
      str
      (subs 1)))

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
      string/upper-case-first))

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
