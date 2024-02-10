(ns heraldicon.util.encoding
  (:require
   [cljs.reader :as reader]
   [cognitect.transit :as transit]
   [heraldicon.math.vector :as v]))

(defn encode-json
  [data]
  (-> data clj->js js/JSON.stringify))

(defn decode-json
  [data]
  (-> data js/JSON.parse (js->clj :keywordize-keys true)))

(def ^:private transit-reader
  (transit/reader :json))

(def ^:private transit-writer
  (transit/writer :json))

(defn encode-transit
  [data]
  (transit/write transit-writer data))

(defn decode-transit
  [data]
  (transit/read transit-reader data))

(defn encode-edn
  [data]
  (prn-str data))

(defn decode-edn
  [data]
  (reader/read-string {:readers {'heraldicon.math.vector.Vector v/map->Vector}} data))

(defn for-mimetype
  [mimetype]
  (let [format (case mimetype
                 "application/json" :json
                 "application/transit+json" :transit
                 :edn)]
    {:content-type (case format
                     :json "application/json"
                     :transit "application/transit+json"
                     "application/edn")

     :encode-fn (case format
                  :json encode-json
                  :transit encode-transit
                  encode-edn)

     :decode-fn (case format
                  :json decode-json
                  :transit decode-transit
                  decode-edn)}))
