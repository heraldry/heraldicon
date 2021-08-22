(ns heraldry.vector.filter)

(def shadow
  [:filter#shadow {:x "-20%"
                   :y "-20%"
                   :width "200%"
                   :height "200%"}
   [:feOffset {:result "offsetOut"
               :in "SourceAlpha"
               :dx "0.2"
               :dy "0.2"}]
   [:feGaussianBlur {:result "blurOut"
                     :in "offsetOut"
                     :std-deviation "1"}]
   [:feBlend {:in "SourceGraphic"
              :in2 "blurOut"
              :mode "normal"}]])
