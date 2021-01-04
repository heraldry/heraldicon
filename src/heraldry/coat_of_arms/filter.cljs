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

(def shiny
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

(def glow
  [:filter {:id     "glow"
            :x      "-100%"
            :y      "-100%"
            :width  "300%"
            :height "300%"}
   [:feColorMatrix {:in     "SourceAlpha"
                    :type   "matrix"
                    :values "0 0 0 1 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0"
                    :result "colorOut"}]
   [:feGaussianBlur {:in           "colorOut"
                     :result       "blurOut"
                     :stdDeviation "10"}]
   [:feBlend {:in   "SourceGraphic"
              :in2  "glowOut"
              :mode "normal"}]])
