(ns or.coad.main
  (:require [goog.string.format]  ;; required for release build
            [hodgepodge.core :refer [local-storage clear!]]
            [or.coa.division :as division]
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
   (merge {:coat-of-arms {:division   {:type  :per-pale
                                       :parts [:azure :sable :or]}
                          :ordinaries [{:type    :chief
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
  [:filter#shadow {:x      "-20%"
                   :y      "-20%"
                   :width  "200%"
                   :height "200%"}
   [:feOffset {:result "offsetOut"
               :in     "SourceAlpha"
               :dx     "1"
               :dy     "1"}]
   [:feGaussianBlur {:result        "blurOut"
                     :in            "offsetOut"
                     :std-deviation "5"}]
   [:feBlend {:in   "SourceGraphic"
              :in2  "blurOut"
              :mode "normal"}]])

(def filter-shiny
  [:filter#shiny {:x      0
                  :y      0
                  :width  "150%"
                  :height "150%"}
   [:feGaussianBlur {:std-deviation 4
                     :in            "SourceAlpha"
                     :result        "blur1"}]
   [:feSpecularLighting {:specular-exponent 20
                         :lighting-color    "#696969"
                         :in                "blur1"
                         :result            "specOut"}
    [:fePointLight {:x 300
                    :y 300
                    :z 500}]]
   [:feComposite {:k1       0
                  :k2       1
                  :k3       1
                  :k4       0
                  :operator "arithmetic"
                  :in       "SourceGraphic"
                  :in2      "specOut"
                  :result   "highlight"}]
   [:feOffset {:dx     14
               :dy     14
               :in     "SourceGraphic"
               :result "offOut"}]
   [:feColorMatrix {:values "0.2 0 0 0 0 0 0.2 0 0 0 0 0 0.2 0 0 0 0 0 1 0"
                    :type   "matrix"
                    :in     "offOut"
                    :result "matrixOut"}]
   [:feGaussianBlur {:std-deviation 10
                     :in            "matrixOut"
                     :result        "blurOut"}]
   [:feBlend {:mode "normal"
              :in   "highlight"
              :in2  "blurOut"}]])

(def mask-shield1
  [:mask#mask-shield1
   [:path {:d    "m 0.0686411,0 v 80 c -0.250464,4.311115 0.123329,8.433503 2.147464,12.513639 2.024135,4.080137 5.344042,7.998631 9.7971239,11.563581 4.453081,3.56494 10.03788,6.77517 16.480874,9.47345 6.442994,2.69828 13.742073,4.88374 21.539758,6.44933 7.797685,-1.56559 15.096765,-3.75105 21.539759,-6.44933 6.442994,-2.69828 12.027792,-5.90851 16.480874,-9.47345 4.453082,-3.56495 7.772988,-7.483444 9.797123,-11.563581 C 99.875753,88.433503 100.01361,84.193063 99.999081,80 V 0 Z"
           :fill "#FFFFFF"}]])

(def defs
  (into
   [:defs
    filter-shadow
    filter-shiny
    mask-shield1]))

(defn render-ordinary [ordinary]
  [:<>])

(defn render-coat-of-arms [data]
  (let [division   (:division data)
        ordinaries (:ordinaries data)]
    [:<>
     [division/render division]
     (for [[idx ordinary] (map-indexed vector ordinaries)]
       ^{:key idx} [render-ordinary ordinary])]))

(defn render-shield [coat-of-arms]
  [:g {:filter "url(#shadow)"}
   [:g {:transform "translate(10,10) scale(5,5)"}
    [:g {:mask "url(#mask-shield1)"}
     [:rect {:x      0
             :y      0
             :width  110
             :height 130
             :fill   "#f0f0f0"}]
     [:g {:transform "translate(50,50)"}
      [render-coat-of-arms coat-of-arms]]]]])

(defn controls [coat-of-arms]
  [:div.controls {}
   [:fieldset
    [:label {:for "division"} "Division"]
    [:select {:name      "division"
              :id        "division"
              :value     (name (get-in coat-of-arms [:division :type]))
              :on-change #(rf/dispatch [:set-in [:coat-of-arms :division :type] (keyword (-> % .-target .-value))])}
     (for [[key display-name] division/options]
       ^{:key key}
       [:option {:value (name key)} display-name])]]])

(defn app []
  (fn []
    (let [coat-of-arms @(rf/subscribe [:get :coat-of-arms])]
      [:<>
       [:div {:style {:width    "100%"
                      :height   "100vh"
                      :position "relative"}}
        [:svg {:id                  "svg"
               :style               {:width    "60%"
                                     :position "absolute"
                                     :left     0
                                     :top      0}
               :viewBox             "0 0 600 1000"
               :preserveAspectRatio "xMidYMin slice"}
         defs
         [render-shield coat-of-arms]]
        [:div {:style {:width    "40%"
                       :position "absolute"
                       :left     "60%"
                       :top      0}}
         [controls coat-of-arms]]]])))

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
