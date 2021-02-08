(ns heraldry.coat-of-arms.tincture
  (:require [heraldry.coat-of-arms.hatching :as hatching]
            [heraldry.util :as util]))

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
(def theme-wappenwiki
  {;; metals
   :argent "#f6f6f6"
   :argent-dark "#e5e5e5"
   :or "#f2bc51"
   ;; colours
   :azure "#0c6793"
   :vert "#3e933f"
   :gules "#bb2f2e"
   :sable "#333333"
   :purpure "#913a6a"
   ;; stains
   :murrey "#a32c45"
   :sanguine "#a42f2d"
   :tenne "#bf7433"
   ;; untraditional
   :carnation "#e9bfa2"
   :brunatre "#725942"
   :cendree "#cbcaca"
   :rose "#d99292"
   :celeste "#359db7"
   :orange "#e58a39"})

;; fallback theme, taken from theme-wappenwiki
(def theme-default
  {;; metals
   :argent "#f6f6f6"
   :argent-dark "#e5e5e5"
   :or "#f2bc51"
   ;; colours
   :azure "#0c6793"
   :vert "#3e933f"
   :gules "#bb2f2e"
   :sable "#333333"
   :purpure "#913a6a"
   ;; stains
   :murrey "#a32c45"
   :sanguine "#a42f2d"
   :tenne "#bf7433"
   ;; untraditional
   :carnation "#e9bfa2"
   :brunatre "#725942"
   :cendree "#cbcaca"
   :rose "#d99292"
   :celeste "#359db7"
   :orange "#e58a39"})

(def theme-web
  {;; metals
   :argent "#ffffff"
   :or "#ffd700"
   ;; colours
   :azure "#0000ff"
   :vert "#008000"
   :gules "#ff0000"
   :sable "#000000"
   :purpure "#800080"
   ;; stains
   :murrey "#5f005f" ;; 30% darkened purpure
   :sanguine "#b20000" ;; 30% darkened gules
   :tenne "#c67000"})

;; Wikipedia themes: https://commons.wikimedia.org/wiki/Template:Tincture

(def theme-wikipedia-default
  {;; metals
   :argent "#ffffff"
   :argent-dark "#e7e7e7"
   :or "#fcdd09"
   ;; colours
   :azure "#0f47af"
   :vert "#078930"
   :gules "#da121a"
   :sable "#000000"
   :purpure "#9116a1"
   ;; stains
   :murrey "#650f70" ;; 30% darkened purpure
   :sanguine "#980c12" ;; 30% darkened gules
   :tenne "#9d5333"
   ;; untraditional
   :celeste "#89c5e3"
   :carnation "#f2a772"
   :cendree "#999999"
   :orange "#eb7711"})

(def theme-wikipedia-web
  {;; metals
   :argent "#ffffff"
   :argent-dark "#eeeeee"
   :or "#ffdd11"
   ;; colours
   :azure "#1177cc"
   :vert "#119933"
   :gules "#dd2222"
   :sable "#000000"
   :purpure "#992288"
   ;; stains
   :murrey "#6b175f" ;; 30% darkened purpure
   :sanguine "#9a1717" ;; 30% darkened gules
   :tenne "#884411"
   ;; untraditional
   :celeste "#33aaee"
   :carnation "#eebb99"
   :cendree "#779988"
   :orange "#ff6600"})

(def theme-wikipedia-bajuvarian
  {;; metals
   :argent "#ffffff"
   :argent-dark "#dddde6"
   :or "#ffd700"
   ;; colours
   :azure "#0033ff"
   :vert "#009900"
   :gules "#ff0000"
   :sable "#000000"
   :purpure "#880088"
   ;; stains
   :murrey "#5f005f" ;; 30% darkened purpure
   :sanguine "#b20000" ;; 30% darkened gules
   :tenne "#884411"
   ;; untraditional
   :celeste "#0099ff"
   :carnation "#ffbb99"
   :cendree "#778899"
   :orange "#eb7711"})

(def theme-wikipedia-brandenburg
  {;; metals
   :argent "#ffffff"
   :or "#f5d306"
   ;; colours
   :azure "#0296C6"
   :vert "#0ca644"
   :gules "#e64625"
   :sable "#000000"
   ;; stains
   :sanguine "#a72c13" ;; 30% darkened gules
   :tenne "#884411"
   ;; untraditional
   :celeste "#89c5e3"})

(def theme-wikipedia-wuerttemberg
  {;; metals
   :argent "#ffffff"
   :or "#fcdb00"
   ;; colours
   :azure "#005198"
   :vert "#0ca644"
   :gules "#da251d"
   :sable "#000000"
   ;; stains
   :sanguine "#981914" ;; 30% darkened gules
   :tenne "#884411"
   ;; untraditional
   :celeste "#0081c9"
   :carnation "#f2b398"})

(def theme-wikipedia-switzerland
  {;; metals
   :argent "#ffffff"
   :or "#ffd72e"
   ;; colours
   :azure "#248bcc"
   :vert "#00a94d"
   :gules "#e7423f"
   :sable "#000000"
   ;; stains
   :sanguine "#b61916" ;; 30% darkened gules
   })

(def theme-wikipedia-spain
  {;; metals
   :argent "#ffffff"
   :argent-dark "#e7e7e7"
   :or "#eac102"
   ;; colours
   :azure "#0071bc"
   :vert "#008f4c"
   :gules "#ed1c24"
   :sable "#000000"
   :purpure "#630b57"
   ;; stains
   :murrey "#45073c" ;; 30% darkened purpure
   :sanguine "#ab0d13" ;; 30% darkened gules
   ;; untraditional
   :carnation "#fcd3bc"
   :orange "#ff6600"})

(def theme-wikipedia-hungary
  {;; metals
   :argent "#ffffff"
   :argent-dark "#ececec"
   :or "#ffd200"
   ;; colours
   :azure "#0039a6"
   :vert "#009a3d"
   :gules "#dc281e"
   :sable "#000000"
   :purpure "#b60a9b"
   ;; stains
   :murrey "#7f066c" ;; 30% darkened purpure
   :sanguine "#9a1c15" ;; 30% darkened gules
   })

(def theme-wikipedia-sweden
  {;; metals
   :argent "#eeeeee"
   :argent-dark "#dddddd"
   :or "#ffcd50"
   ;; colours
   :azure "#003d8f"
   :vert "#225500"
   :gules "#d40000"
   :sable "#000000"
   :purpure "#aa235a"
   ;; stains
   :murrey "#76183f" ;; 30% darkened purpure
   :sanguine "#940000" ;; 30% darkened gules
   ;; untraditional
   :celeste "#3291d7"
   :carnation "#eebb99"})

(def theme-ral-traffic
  {;; metals
   :argent "#f6f6f6" ;; RAL 9016
   :argent-dark "#929899" ;; RAL 7042
   :or "#f0ca00" ;; RAL 1023
   ;; colours
   :azure "#004c91" ;; RAL 5017
   :vert "#008351" ;; RAL 6024
   :gules "#bf111b" ;; RAL 3020
   :sable "#2a292a" ;; RAL 9017
   :purpure "#912d76" ;; RAL 4006
   ;; stains
   :murrey "#5f005f" ;; 30% darkened purpure
   :sanguine "#b20000" ;; 30% darkened gules
   :tenne "#884411"
   ;; untraditional
   :celeste "#0099ff"
   :carnation "#ffbb99"
   :cendree "#4f5250" ;; RAL 7043
   :orange "#de5307" ;; RAL 2009
   })

;; https://fr.wikipedia.org/wiki/Projet:Blasons/Cr%C3%A9ation#Unification_des_couleurs

(def theme-wikipedia-france
  {;; metals
   :argent "#ffffff"
   :or "#fcef3c"
   ;; colours
   :azure "#2b5df2"
   :vert "#5ab532"
   :gules "#e20909"
   :sable "#000000"
   :purpure "#d576ad"
   ;; stains
   :murrey "#570b63"
   :sanguine "#a41619"
   :tenne "#9d5324"})

(def themes
  [["General"
    ["WappenWiki (default)" :wappenwiki theme-wappenwiki]
    ["Web" :theme-web theme-web]
    ["RAL Traffic" :ral-traffic theme-ral-traffic]]
   ["Wikipedia"
    ["Wikipedia default" :wikipedia-default theme-wikipedia-default]
    ["Wikipedia web" :wikipedia-web theme-wikipedia-web]
    ["Wikipedia Bajuvarian" :wikipedia-bajuvarian theme-wikipedia-bajuvarian]
    ["Wikipedia Brandenburg" :wikipedia-brandenburg theme-wikipedia-brandenburg]
    ["Wikipedia Württemberg" :wikipedia-wurttemberg theme-wikipedia-wuerttemberg]
    ["Wikipedia France" :wikipedia-france theme-wikipedia-france]
    ["Wikipedia Hungary" :wikipedia-hungary theme-wikipedia-hungary]
    ["Wikipedia Spain" :wikipedia-spain theme-wikipedia-spain]
    ["Wikipedia Sweden" :wikipedia-sweden theme-wikipedia-sweden]
    ["Wikipedia Switzerland" :wikipedia-switzerland theme-wikipedia-switzerland]]])

(def default-theme
  :wappenwiki)

(def theme-choices
  (->> themes
       (map (fn [[group-name & items]]
              (vec (concat [group-name] (->> items
                                             (map (fn [[display-name key _]]
                                                    [display-name key])))))))
       vec))

(def theme-map
  (util/choices->map theme-choices))

(def theme-data-map
  (->> themes
       (map (fn [[_group-name & items]]
              (->> items
                   (map (fn [[_ key colours]]
                          [key colours])))))
       (apply concat)
       (into {})))

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

(def tincture-map
  (util/choices->map choices))

(def fixed-tincture-choices
  (concat [["None (can be changed)" :none]
           ["Proper" :proper]]
          choices))

(def fixed-tincture-map
  (util/choices->map fixed-tincture-choices))

(defn lookup-colour [tincture theme]
  (let [theme-colours (merge
                       (get theme-data-map default-theme)
                       (get theme-data-map theme))]
    (get theme-colours tincture)))

(def ermine
  ["ermine" :argent :sable])

(def ermines
  ["ermines" :sable :argent])

(def erminois
  ["erminois" :or :sable])

(def pean
  ["pean" :sable :or])

(def furs
  {:ermine ermine
   :ermines ermines
   :erminois erminois
   :pean pean})

(defn pick [tincture {:keys [mode theme]}]
  (cond
    (= tincture :none) "url(#void)"
    (get furs tincture) (let [[id _ _] (get furs tincture)]
                          (str "url(#" id ")"))
    (= mode :hatching) (or
                        (hatching/get-for tincture)
                        "#888")
    :else (or (lookup-colour tincture theme)
              (get furs tincture)
              "url(#void)")))

(defn patterns [{:keys [theme]}]
  (into
   [:<>
    void
    selected]
   (for [[id background foreground] (vals furs)]
     (ermine-base
      id
      (lookup-colour background theme)
      (lookup-colour foreground theme)))))
