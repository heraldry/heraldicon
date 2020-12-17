(ns or.coad.main
  (:require [goog.string.format]  ;; required for release build
            [hodgepodge.core :refer [local-storage clear!]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

                                        ; subs

(rf/reg-sub
 :get
 (fn [db [_ key]]
   (key db)))

                                        ; events

(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {:coat-of-arms {:division {:type :per-pale
                                     :parts [:azure :sable :or]}
                          :ordinaries [{:type :chief
                                        :content :or}]}} db)))

(defn save-state [db]
  (assoc! local-storage :coat-of-arms (:coat-of-arms db)))

(rf/reg-event-db
 :set
 (fn [db [_ key value]]
   (assoc db key value)))

(rf/reg-event-db
 :set-in
 (fn [db [_ path value]]
   (println path value)
   (assoc-in db path value)))
                                        ; views

(def filter-shadow
  [:filter#shadow {:x "-20%"
                   :y "-20%"
                   :width "200%"
                   :height "200%"}
   [:feOffset {:result "offsetOut"
               :in "SourceAlpha"
               :dx "1"
               :dy "1"}]
   [:feGaussianBlur {:result "blurOut"
                     :in "offsetOut"
                     :std-deviation "5"}]
   [:feBlend {:in "SourceGraphic"
              :in2 "blurOut"
              :mode "normal"}]])

(def filter-shiny
  [:filter#shiny {:x 0
                  :y 0
                  :width "150%"
                  :height "150%"}
   [:feGaussianBlur {:std-deviation 4
                     :in "SourceAlpha"
                     :result "blur1"}]
   [:feSpecularLighting {:specular-exponent 20
                         :lighting-color "#696969"
                         :in "blur1"
                         :result "specOut"}
    [:fePointLight {:x 300
                    :y 300
                    :z 500}]]
   [:feComposite {:k1 0
                  :k2 1
                  :k3 1
                  :k4 0
                  :operator "arithmetic"
                  :in "SourceGraphic"
                  :in2 "specOut"
                  :result "highlight"}]
   [:feOffset {:dx 14
               :dy 14
               :in "SourceGraphic"
               :result "offOut"}]
   [:feColorMatrix {:values "0.2 0 0 0 0 0 0.2 0 0 0 0 0 0.2 0 0 0 0 0 1 0"
                    :type "matrix"
                    :in "offOut"
                    :result "matrixOut"}]
   [:feGaussianBlur {:std-deviation 10
                     :in "matrixOut"
                     :result "blurOut"}]
   [:feBlend {:mode "normal"
              :in "highlight"
              :in2 "blurOut"}]])

(def mask-shield1
  [:mask#mask-shield1
   [:path {:d "m 0.0686411,0 v 80 c -0.250464,4.311115 0.123329,8.433503 2.147464,12.513639 2.024135,4.080137 5.344042,7.998631 9.7971239,11.563581 4.453081,3.56494 10.03788,6.77517 16.480874,9.47345 6.442994,2.69828 13.742073,4.88374 21.539758,6.44933 7.797685,-1.56559 15.096765,-3.75105 21.539759,-6.44933 6.442994,-2.69828 12.027792,-5.90851 16.480874,-9.47345 4.453082,-3.56495 7.772988,-7.483444 9.797123,-11.563581 C 99.875753,88.433503 100.01361,84.193063 99.999081,80 V 0 Z"
           :fill "#FFFFFF"}]])

(def defs
  (into
   [:defs
    filter-shadow
    filter-shiny
    mask-shield1]))

(def tinctures
  {:or "#f1b952"
   :azure "#1b6690"
   :vert "#429042"
   :gules "#b93535"
   :argent "#f5f5f5"
   :sable "#373737"
   :purpure "#8f3f6a"
   :murrey "#8f3f6a"
   :sanguine "#b93535"
   :carnation "#e9bea1"
   :brunatre "#725a44"
   :cendree "#cbcaca"
   :rose "#e9bea1"
   :celestial-azure "#50bbf0"
   :tenne "#725a44"
   :orange "#e56411"
   :iron "#cbcaca"
   :bronze "#f1b952"
   :copper "#f1b952"
   :lead "#cbcaca"
   :steel "#cbcaca"
   :white "#f5f5f5"})

(defn base-area [fill]
  [:rect {:x -1000
          :y -1000
          :width 2000
          :height 2000
          :fill fill}])

;; TODO: masks for each field with a defined center and a scale
(defn per-pale [[left right]]
  [:<>
   [base-area (get tinctures left)]
   [:path {:d "m 0,-1000 h 2000 v 2000 h -2000 z"
           :fill (get tinctures right)}]])

(defn per-fess [[top bottom]]
  [:<>
   [base-area (get tinctures top)]
   [:path {:d "m -1000,0 h 2000 v 2000 h -2000 z"
           :fill (get tinctures bottom)}]])

(defn per-bend [[top bottom]]
  [:<>
   [base-area (get tinctures top)]
   [:path {:d "m -1000,-1000 v 2000 h 2000 z"
           :fill (get tinctures bottom)}]])

(defn per-bend-sinister [[top bottom]]
  [:<>
   [base-area (get tinctures top)]
   [:path {:d "m 1000,-1000 v 2000 h -2000 z"
           :fill (get tinctures bottom)}]])

(defn per-chevron [[top bottom]]
  [:<>
   [base-area (get tinctures top)]
   [:path {:d "m 0,0 l 1000,1000 h -2000 z"
           :fill (get tinctures bottom)}]])

(defn per-saltire [[vertical horizontal]]
  [:<>
   [base-area (get tinctures vertical)]
   [:path {:d "m 0,0 l -1000,-1000 v 2000 z"
           :fill (get tinctures horizontal)}]
   [:path {:d "m 0,0 l 1000,-1000 v 2000 z"
           :fill (get tinctures horizontal)}]])

(defn quarterly [[left right]]
  [:<>
   [base-area (get tinctures left)]
   [:path {:d "m 0,0 h 1000 v -1000 h -1000 z"
           :fill (get tinctures right)}]
   [:path {:d "m 0,0 h -1000 v 1000 h 1000 z"
           :fill (get tinctures right)}]])

(defn gyronny [[left right]]
  [:<>
   [base-area (get tinctures left)]
   [:path {:d "m 0,0 v -1000 h 1000 z"
           :fill (get tinctures right)}]
   [:path {:d "m 0,0 h 1000 v 1000 z"
           :fill (get tinctures right)}]
   [:path {:d "m 0,0 v 1000 h -1000 z"
           :fill (get tinctures right)}]
   [:path {:d "m 0,0 h -1000 v -1000 z"
           :fill (get tinctures right)}]])

(defn tierced-in-pale [[left middle right]]
  [:<>
   [base-area (get tinctures left)]
   [:path {:d "m -16.666666,-1000 h 2000 v 2000 h -2000 z"
           :fill (get tinctures middle)}]
   [:path {:d "m 16.666666,-1000 h 2000 v 2000 h -2000 z"
           :fill (get tinctures right)}]])

(defn tierced-in-fesse [[top middle bottom]]
  [:<>
   [base-area (get tinctures top)]
   [:path {:d "m -1000,-16.666666 h 2000 v 2000 h -2000 z"
           :fill (get tinctures middle)}]
   [:path {:d "m -1000,16.666666 h 2000 v 2000 h -2000 z"
           :fill (get tinctures bottom)}]])

(defn tierced-in-pairle [[left right bottom]]
  [:<>
   [base-area (get tinctures left)]
   [:path {:d "m 0,-1000 h 2000 v 2000 h -2000 z"
           :fill (get tinctures right)}]
   [:path {:d "m 0,0 l 1000,1000 h -2000 z"
           :fill (get tinctures bottom)}]])

;; TODO: offset and/or number of stripes to fit, which dictates their width
(defn paly [[base stripe]]
  [:<>
   [base-area (get tinctures base)]
   [:path {:d "m -37.5,-1000 h 12.5 v 2000 h -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -12.5,-1000 h 12.5 v 2000 h -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m 12.5,-1000 h 12.5 v 2000 h -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m 37.5,-1000 h 12.5 v 2000 h -12.5 z"
           :fill (get tinctures stripe)}]])

(defn barry [[base stripe]]
  [:<>
   [base-area (get tinctures base)]
   [:path {:d "m -1000,-37.5 v 12.5 h 2000 v -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,-12.5 v 12.5 h 2000 v -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,12.5 v 12.5 h 2000 v -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,37.5 v 12.5 h 2000 v -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,62.5 v 12.5 h 2000 v -12.5 z"
           :fill (get tinctures stripe)}]])

(defn bendy [[base stripe]]
  [:<>
   [base-area (get tinctures base)]
   [:path {:d "m -1000,-1000 v 25 l 2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,-1050 v 25 l 2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,-1100 v 25 l 2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,-950 v 25 l 2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,-900 v 25 l 2000,2000 v -25 z"
           :fill (get tinctures stripe)}]])

(defn bendy-sinister [[base stripe]]
  [:<>
   [base-area (get tinctures base)]
   [:path {:d "m 1000,-1025 v 25 l -2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m 1000,-1075 v 25 l -2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m 1000,-1125 v 25 l -2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m 1000,-975 v 25 l -2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m 1000,-925 v 25 l -2000,2000 v -25 z"
           :fill (get tinctures stripe)}]])

(defn render-division [{:keys [type parts]}]
  (case type
    :per-pale [per-pale parts]
    :per-fess [per-fess parts]
    :per-bend [per-bend parts]
    :per-bend-sinister [per-bend-sinister parts]
    :per-chevron [per-chevron parts]
    :per-saltire [per-saltire parts]
    :quarterly [quarterly parts]
    :gyronny [gyronny parts]
    :tierced-in-pale [tierced-in-pale parts]
    :tierced-in-fesse [tierced-in-fesse parts]
    :tierced-in-pairle [tierced-in-pairle parts]
    :paly [paly parts]
    :barry [barry parts]
    :bendy [bendy parts]
    :bendy-sinister [bendy-sinister parts]
    [:<>]))

(defn render-ordinary [ordinary]
  [:<>])

(defn render-coat-of-arms [data]
  (let [division (:division data)
        ordinaries (:ordinaries data)]
    [:<>
     [render-division division]
     (for [[idx ordinary] (map-indexed vector ordinaries)]
       ^{:key idx} [render-ordinary ordinary])]))

(defn render-shield [coat-of-arms]
  [:g {:filter "url(#shadow)"}
   [:g {:transform "translate(10,10) scale(5,5)"}
    [:g {:mask "url(#mask-shield1)"}
     [:rect {:x 0
             :y 0
             :width 110
             :height 130
             :fill "#f0f0f0"}]
     [:g {:transform "translate(50,50)"}
      [render-coat-of-arms coat-of-arms]]]]])

(defn app []
  (fn []
    (let [coat-of-arms @(rf/subscribe [:get :coat-of-arms])]
      [:<>
       [:div {:style {:width "100%"
                      :height "100vh"
                      :position "relative"}}
        [:svg {:id "svg"
               :style {:width "60%"
                       :position "absolute"
                       :left 0
                       :top 0}
               :viewBox "0 0 600 1000"
               :preserveAspectRatio "xMidYMin slice"}
         defs
         [render-shield coat-of-arms]]
        [:div {:style {:width "40%"
                       :position "absolute"
                       :left "60%"
                       :top 0}}
         [:label {:for "division"} "Division"]
         [:select {:name "division"
                   :id "division"
                   :value (name (get-in coat-of-arms [:division :type]))
                   :on-change #(rf/dispatch [:set-in [:coat-of-arms :division :type] (keyword (-> % .-target .-value))])}
          [:option {:value "per-pale"} "Per pale"]
          [:option {:value "per-fess"} "Per fess"]
          [:option {:value "per-bend"} "Per bend"]
          [:option {:value "per-bend-sinister"} "Per bend sinister"]
          [:option {:value "per-chevron"} "Per chevron"]
          [:option {:value "per-saltire"} "Per saltire"]
          [:option {:value "quarterly"} "Quarterly"]
          [:option {:value "gyronny"} "Gyronny"]
          [:option {:value "tierced-in-pale"} "Tierced in pale"]
          [:option {:value "tierced-in-fesse"} "Tierced in fesse"]
          [:option {:value "tierced-in-pairle"} "Tierced in pairle"]
          [:option {:value "paly"} "Paly"]
          [:option {:value "barry"} "Barry"]
          [:option {:value "bendy"} "Bendy"]
          [:option {:value "bendy-sinister"} "Bendy sinister"]
          #_[:option {:value "chevronny"} "Chevronny"]]]]])))

(defn stop []
  (println "Stopping..."))

(defn start []
  (rf/dispatch-sync [:initialize-db])
  (if (= js/window.location.search "?reset")
    (do
      (clear! local-storage)
      (set! js/window.location js/window.location.pathname))
    (r/render [app]
              (.getElementById js/document "app"))))

(defn ^:export init []
  (start))
