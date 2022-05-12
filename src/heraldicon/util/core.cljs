(ns heraldicon.util.core
  (:require
   [heraldicon.config :as config]))

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

(defn index-of [item coll]
  (count (take-while (partial not= item) coll)))

(defn avatar-url [username]
  (str (or (config/get :heraldicon-site-url)
           (config/get :heraldicon-url))
       "/avatar/" username))

(defn integer-string? [s]
  (re-matches #"^[0-9]+$" s))
