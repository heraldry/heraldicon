(ns heraldry.coat-of-arms.filter)

(def shadow
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
