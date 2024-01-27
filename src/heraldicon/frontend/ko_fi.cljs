(ns heraldicon.frontend.ko-fi
  (:require
   [heraldicon.static :as static]))

(defn large-button
  []
  [:a {:href "https://ko-fi.com/heraldicon"
       :target "_blank"
       :title "Support Heraldicon on Ko-fi"}
   [:div {:style {:background-color "#0c6793"
                  :color "#fff"
                  :margin-left "auto"
                  :margin-right "auto"
                  :margin-bottom "15px"
                  :width "250px"
                  :border-radius "12px"
                  :text-align "center"
                  :vertical-align "middle"
                  :padding "2px 12px"
                  :box-shadow "1px 1px 0px rgba(0, 0, 0, 0.2)"}}
    [:span {:style {:font-family "'Quicksand', Helvetica, Century Gothic, sans-serif "
                    :font-weight "700"
                    :font-size "14px"
                    :line-height "33px"
                    :text-wrap "none"}}
     [:img {:src "https://storage.ko-fi.com/cdn/cup-border.png"
            :alt "Ko-fi tip"
            :style {:margin-right "8px"
                    :height "15px"
                    :margin-bottom "3px"
                    :vertical-align "middle"
                    :animation "kofi-wiggle 3s infinite"}}]
     "Support Heraldicon on Ko-fi"]]])

(defn small-button
  []
  [:a {:href "https://ko-fi.com/heraldicon"
       :target "_blank"
       :title "Support Heraldicon on Ko-fi"}
   [:img {:src "https://storage.ko-fi.com/cdn/cup-border.png"
          :alt "Ko-fi tip"
          :style {:margin-right "8px"
                  :height "15px"
                  :margin-bottom "3px"
                  :vertical-align "middle"}}]])

(defn qr-code
  []
  [:img {:src (static/static-url "/img/ko-fi-qrcode.png")
         :style {:margin "auto"
                 :display "block"}}])
