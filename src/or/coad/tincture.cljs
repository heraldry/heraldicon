(ns or.coad.tincture
  (:require [or.coad.hatching :as hatching]))

(def tinctures
  {;; metal
   :argent "#f5f5f5"
   :or "#f1b952"
   ;; tincture
   :azure "#1b6690"
   :vert "#429042"
   :gules "#b93535"
   :sable "#373737"
   :purpure "#8f3f6a"
   ;; stains
   :murrey "#8f3f6a"
   :sanguine "#b93535"
   :tenne "#725a44"
   ;; secondary
   :carnation "#e9bea1"
   :brunatre "#725a44"
   :cendree "#cbcaca"
   :rose "#e9bea1"
   :celestial-azure "#50bbf0"
   :orange "#e56411"
   :iron "#cbcaca"
   :bronze "#f1b952"
   :copper "#f1b952"
   :lead "#cbcaca"
   :steel "#cbcaca"
   :white "#f5f5f5"})

(def options
  [["Metal"
    ["Argent" :argent]
    ["Or" :or]]
   ["Tincture"
    ["Azure" :azure]
    ["Gules" :gules]
    ["Purpure" :purpure]
    ["Sable" :sable]
    ["Vert" :vert]]
   ["Stain"
    ["Murrey" :murrey]
    ["Sanguine" :sanguine]
    ["Tenné" :tenne]]
   ["Secondary"
    ["Bronze" :bronze]
    ["Brunatre" :brunatre]
    ["Carnation" :carnation]
    ["Celestial Azure" :celestial-azure]
    ["Cendree" :cendree]
    ["Copper" :copper]
    ["Iron" :iron]
    ["Lead" :lead]
    ["Orange" :orange]
    ["Rose" :rose]
    ["Steel" :steel]
    ["White" :white]]])

(defn pick [tincture {:keys [mode]}]
  (cond
    (= tincture :none) "url(#void)"
    (= mode :colours) (get tinctures tincture)
    (= mode :hatching) (hatching/get-for tincture)))
