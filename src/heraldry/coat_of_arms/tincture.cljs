(ns heraldry.coat-of-arms.tincture
  (:require [heraldry.coat-of-arms.hatching :as hatching]))

(def ermine-shape
  [100 160
   "M 47.757634,0 C 60.324696,15.689314 25.136924,21.898666 44.167046,36.607392 52.24587,41.673746 71.273896,30.723464 54.57765,17.322176 52.423302,16.178163 62.11999,8.3349512 47.757634,0 Z m 21.182372,36.935142 c -3.23154,0.326846 -6.282816,2.290268 -9.155302,7.193166 -3.590588,8.33495 12.209402,22.716724 22.98117,5.06625 C 83.483996,47.069952 94.0755,53.93163 100,39.386326 89.946316,49.845864 79.713166,37.425428 70.377642,36.935142 Z M 29.80469,37.099022 C 20.469164,37.58931 10.053647,50.173186 0,39.55021 5.9244804,54.095512 16.51881,47.070414 17.416458,49.358438 28.0087,67.008916 43.805884,52.627142 40.215296,44.292192 36.983772,38.73555 33.395278,36.9356 29.80469,37.099026 Z m 21.003542,10.13249 c -8.25835,37.262136 -23.51556,63.576958 -40.211787,82.534868 7.001653,-1.96117 14.718613,-7.19213 22.258843,-11.93164 -4.667764,9.80582 -9.154602,18.14082 -13.822364,26.80264 2.692942,-0.98059 11.489886,-9.64435 23.338826,-22.3919 1.795296,17.48703 4.305204,25.8241 8.07532,37.75452 3.052012,-11.93042 5.208468,-19.9411 7.54234,-37.9184 7.181178,8.00805 14.001888,16.17984 23.696484,23.04393 L 68.221186,117.34659 c 7.001634,4.41265 14.72282,10.29262 22.083522,12.74406 C 72.710832,110.15215 57.989412,84.493648 50.808232,47.231516 Z"])

(defn ermine-base [id background foreground]
  (let [width (/ 100 8)
        [shape-width
         shape-height
         shape] ermine-shape
        spot-width (/ width 2)
        scale (/ spot-width shape-width)
        spot-height (* shape-height scale)
        height (* spot-height 2)]
    [:pattern {:id id
               :width width
               :height height
               :pattern-units "userSpaceOnUse"
               :x (/ spot-width -2)
               :y (/ spot-height -2)}
     [:rect {:x 0
             :y 0
             :width width
             :height height
             :fill background}]
     [:g {:fill foreground}
      [:path {:d shape
              :transform (str "scale(" scale "," scale ")")}]
      [:path {:d shape
              :transform (str "translate(" spot-width "," spot-height ") scale(" scale "," scale ")")}]]]))

(def ermine
  (let [id "ermine"]
    [id (ermine-base id "#fff" "#000")]))

(def ermines
  (let [id "ermines"]
    [id (ermine-base id "#000" "#fff")]))

(def erminois
  (let [id "erminois"]
    [id (ermine-base id "#f1b952" "#000")]))

(def pean
  (let [id "pean"]
    [id (ermine-base id "#000" "#f1b952")]))

(def void
  [:pattern#void {:width 20
                  :height 20
                  :pattern-units "userSpaceOnUse"}
   [:rect {:x 0
           :y 0
           :width 20
           :height 20
           :fill "#fff"}]
   [:rect {:x 0
           :y 0
           :width 10
           :height 10
           :fill "#ddd"}]
   [:rect {:x 10
           :y 10
           :width 10
           :height 10
           :fill "#ddd"}]])

(def selected
  (let [spacing 2
        width (* spacing 2)
        size 0.3]
    [:pattern#selected {:width width
                        :height width
                        :pattern-units "userSpaceOnUse"}
     [:rect {:x 0
             :y 0
             :width width
             :height width
             :fill "#f5f5f5"}]
     [:g {:fill "#000"}
      [:circle {:cx 0
                :cy 0
                :r size}]
      [:circle {:cx width
                :cy 0
                :r size}]
      [:circle {:cx 0
                :cy width
                :r size}]
      [:circle {:cx width
                :cy width
                :r size}]
      [:circle {:cx spacing
                :cy spacing
                :r size}]]]))

;; colours taken from ;; https://github.com/drawshield/Drawshield-Code/blob/0d7ecc865e5ccd2ae8b17c11511c6afebb2ff6c9/svg/schemes/wappenwiki.txt
(def metals
  {:argent "#f5f5f5"
   :or "#f1b952"})

(def colours
  {:azure "#1b6690"
   :vert "#429042"
   :gules "#b93535"
   :sable "#373737"
   :purpure "#8f3f6a"
   ;; stains
   :murrey "#6b2f4f"
   :sanguine "#8a2727"
   :tenne "#725a44"
   ;; untraditional
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

(def furs
  {:ermine ermine
   :ermines ermines
   :erminois erminois
   :pean pean})

(def choices
  [["Metal"
    ["Argent" :argent]
    ["Or" :or]]
   ["Colour"
    ["Azure" :azure]
    ["Gules" :gules]
    ["Purpure" :purpure]
    ["Sable" :sable]
    ["Vert" :vert]]
   ["Fur"
    ["Ermine" :ermine]
    ["Ermines" :ermines]
    ["Erminois" :erminois]
    ["Pean" :pean]]
   ["Stain"
    ["Sanguine" :sanguine]
    ["Murrey" :murrey]
    ["Tenné" :tenne]]])

(defn pick [tincture {:keys [mode]}]
  (cond
    (= tincture :none) "url(#void)"
    (get furs tincture) (let [[id _] (get furs tincture)]
                          (str "url(#" id ")"))
    (= mode :hatching) (or
                        (hatching/get-for tincture)
                        "#888")
    :else (or (get metals tincture)
              (get colours tincture)
              (get furs tincture))))

(def patterns
  (into
   [:<>
    void
    selected]
   (for [[_ pattern] (vals furs)]
     pattern)))
