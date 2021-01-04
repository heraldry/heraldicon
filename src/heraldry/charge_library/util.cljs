(ns heraldry.charge-library.util
  (:require [cljs.reader :as reader]
            [clojure.pprint :refer [pprint]]
            [taoensso.timbre :as log]))

(log/merge-config!
 {:middleware [(fn [data]
                 (update data :vargs (partial mapv #(if (string? %)
                                                      %
                                                      (str "\n" (with-out-str (pprint %)))))))]})

(defn time-now []
  (/ (js/Date.now) 1000.0))

(defn timestamp []
  (.toISOString (js/Date.)))

(defn parse-timestamp [s]
  (/ (js/Date.parse s) 1000.0))

(defn clone-js [jsobj]
  (js->clj (.parse js/JSON (.stringify js/JSON jsobj)) :keywordize-keys true))

(defn map-keys
  "Applies f to each key of m. Also to keys of m's vals and so on."
  [f m]
  (zipmap
   (map (fn [k]
          (f k))
        (keys m))
   (map (fn [v]
          (if (map? v)
            (map-keys f v)
            v))
        (vals m))))

(defn promise
  [resolver]
  (js/Promise. resolver))

(defn promise-from-callback
  [f]
  (promise (fn [resolve reject]
             (f (fn [error data]
                  (if (nil? error)
                    (resolve data)
                    (reject error)))))))

(defn parse-ws-payload [payload]
  (-> payload
      reader/read-string))

(defn ws-payload [data]
  (-> (pr-str data)))

(defn pp [& args]
  (apply js/console.log args))

(defn indices-of [f coll]
  (keep-indexed #(if (f %2)
                   %1
                   nil) coll))

(defn first-index-of [f coll]
  (first (indices-of f coll)))

(defn index-of [value coll]
  (first-index-of #(= % value) coll))

(defn encode-base64 [data]
  (try
    (js/window.btoa data)
    (catch js/ReferenceError _
      (-> data (js/Buffer.) (.toString "base64")))))

(defn decode-base64 [data]
  (try
    (js/window.atob data)
    (catch js/ReferenceError _
      (-> data (js/Buffer. "base64") .toString))))

(defn db-identifier [type id]
  (str type "#" id))

(defn connection-identifier [id]
  (db-identifier "connection" id))

(defn subscription-identifier [id]
  (db-identifier "subscription" id))

(defn user-identifier [id]
  (db-identifier "user" id))

(defn user-lookup-identifier [lookup]
  (db-identifier "user-lookup" lookup))
