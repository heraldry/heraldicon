(ns or.coad.ordinary
  (:require [or.coad.tinctures :refer [tinctures]]))

(defn pale [tincture]
  [:<>])

(defn fess [tincture]
  [:<>])

(defn bend [tincture]
  [:<>])

(defn bend-sinister [tincture]
  [:<>])

(defn chief [tincture]
  [:<>])

(defn base [tincture]
  [:<>])

(defn cross [tincture]
  [:<>])

(defn saltire [tincture]
  [:<>])

(defn chevron [tincture]
  [:<>])

(defn pall [tincture]
  [:<>])

(def kinds
  [["Pale" :pale pale]
   ["Fesse" :fess fess]
   ["Bend" :bend bend]
   ["Bend Sinister" :bend-sinister bend-sinister]
   ["Chief" :per-chevron chief]
   ["Base" :base base]
   ["Cross" :cross cross]
   ["Saltire" :saltire saltire]
   ["Chevron" :chevron chevron]
   ["Pall" :pall pall]])

(def kinds-function-map
  (->> kinds
       (map (fn [[_ key function]]
              [key function]))
       (into {})))

(def options
  (->> kinds
       (map (fn [[name key _]]
              [key name]))))

(defn render [{:keys [type]}]
  (let [function (get kinds-function-map type)]
    [function]))
