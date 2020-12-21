(ns or.coad.hatching)

(def spacing 2.5)
(def line-thickness 0.2)

(def argent
  (let [id "hatching-argent"]
    [id
     (let [width 10]
       [:pattern {:id id
                  :width width
                  :height width
                  :pattern-units "userSpaceOnUse"}
        [:rect {:x 0
                :y 0
                :width width
                :height width
                :fill "#f5f5f5"}]])]))

#_{:clj-kondo/ignore [:redefined-var]}
(def or
  (let [id "hatching-or"]
    [id
     (let [width (* spacing 2)
           size 0.5]
       [:pattern {:id id
                  :width width
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
                   :r size}]]])]))

(def azure
  (let [id "hatching-azure"]
    [id
     (let [width (* spacing 2)]
       [:pattern {:id id
                  :width width
                  :height width
                  :pattern-units "userSpaceOnUse"}
        [:rect {:x 0
                :y 0
                :width width
                :height width
                :fill "#f5f5f5"}]
        [:g {:stroke "#000"
             :stroke-width line-thickness}
         [:path {:d (str "M" 0 "," 0 " L" width "," 0)}]
         [:path {:d (str "M" 0 "," spacing " L" width "," spacing)}]
         [:path {:d (str "M" 0 "," width " L" width "," width)}]]])]))

(def vert
  (let [id "hatching-vert"]
    [id
     (let [width (* (Math/sqrt 2) spacing)]
       [:pattern {:id id
                  :width width
                  :height width
                  :pattern-units "userSpaceOnUse"}
        [:rect {:x 0
                :y 0
                :width width
                :height width
                :fill "#f5f5f5"}]
        [:g {:stroke "#000"
             :stroke-width line-thickness}
         [:path {:d (str "M" (- width) "," (- width) " l" (* width 3) "," (* width 3))}]
         [:path {:d (str "M" 0 "," (- width) " l" (* width 3) "," (* width 3))}]
         [:path {:d (str "M" (- width) "," 0 " l" (* width 3) "," (* width 3))}]]])]))

(def gules
  (let [id "hatching-gules"]
    [id
     (let [width (* spacing 2)]
       [:pattern {:id id
                  :width width
                  :height width
                  :pattern-units "userSpaceOnUse"}
        [:rect {:x 0
                :y 0
                :width width
                :height width
                :fill "#f5f5f5"}]
        [:g {:stroke "#000"
             :stroke-width line-thickness}
         [:path {:d (str "M" 0 "," 0 " L" 0 "," width)}]
         [:path {:d (str "M" spacing "," 0 " L" spacing "," width)}]
         [:path {:d (str "M" width "," 0 " L" width "," width)}]]])]))

(def purpure
  (let [id "hatching-purpure"]
    [id
     (let [width (* (Math/sqrt 2) spacing)]
       [:pattern {:id id
                  :width width
                  :height width
                  :pattern-units "userSpaceOnUse"}
        [:rect {:x 0
                :y 0
                :width width
                :height width
                :fill "#f5f5f5"}]
        [:g {:stroke "#000"
             :stroke-width line-thickness}
         [:path {:d (str "M" (* width 2) "," (- width) " l" (* width -3) "," (* width 3))}]
         [:path {:d (str "M" width "," (- width) " l" (* width -3) "," (* width 3))}]
         [:path {:d (str "M" (* width 2) "," 0 " l" (* width -3) "," (* width 3))}]]])]))

(def sable
  (let [id "hatching-sable"]
    [id
     (let [width (* spacing 2)]
       [:pattern {:id id
                  :width width
                  :height width
                  :pattern-units "userSpaceOnUse"}
        [:rect {:x 0
                :y 0
                :width width
                :height width
                :fill "#f5f5f5"}]
        [:g {:stroke "#000"
             :stroke-width line-thickness}
         [:path {:d (str "M" 0 "," 0 " L" width "," 0)}]
         [:path {:d (str "M" 0 "," spacing " L" width "," spacing)}]
         [:path {:d (str "M" 0 "," width " L" width "," width)}]
         [:path {:d (str "M" 0 "," 0 " L" 0 "," width)}]
         [:path {:d (str "M" spacing "," 0 " L" spacing "," width)}]
         [:path {:d (str "M" width "," 0 " L" width "," width)}]]])]))

(def murrey
  (let [id "hatching-murrey"]
    [id
     (let [width (* (Math/sqrt 2) spacing)]
       [:pattern {:id id
                  :width width
                  :height width
                  :pattern-units "userSpaceOnUse"}
        [:rect {:x 0
                :y 0
                :width width
                :height width
                :fill "#f5f5f5"}]
        [:g {:stroke "#000"
             :stroke-width line-thickness}
         [:path {:d (str "M" (- width) "," (- width) " l" (* width 3) "," (* width 3))}]
         [:path {:d (str "M" 0 "," (- width) " l" (* width 3) "," (* width 3))}]
         [:path {:d (str "M" (- width) "," 0 " l" (* width 3) "," (* width 3))}]
         [:path {:d (str "M" (* width 2) "," (- width) " l" (* width -3) "," (* width 3))}]
         [:path {:d (str "M" width "," (- width) " l" (* width -3) "," (* width 3))}]
         [:path {:d (str "M" (* width 2) "," 0 " l" (* width -3) "," (* width 3))}]]])]))

(def sanguine
  (let [id "hatching-sanguine"]
    [id
     [:<>
      (let [width (* spacing 2)]
        [:pattern {:id "hatching-sanguine-secondary"
                   :width width
                   :height width
                   :pattern-units "userSpaceOnUse"}
         [:g {:stroke "#000"
              :stroke-width line-thickness}
          [:path {:d (str "M" 0 "," 0 " L" width "," 0)}]
          [:path {:d (str "M" 0 "," spacing " L" width "," spacing)}]
          [:path {:d (str "M" 0 "," width " L" width "," width)}]]])

      (let [width 150
            height 200]
        [:pattern {:id id
                   :width width
                   :height height
                   :pattern-units "userSpaceOnUse"}
         [:defs]

         [:rect {:x 0
                 :y 0
                 :width width
                 :height height
                 :fill "#f5f5f5"}]
         [:rect {:x 0
                 :y 0
                 :width 1000
                 :height 1000
                 :fill "url(#hatching-vert)"}]
         [:rect {:x 0
                 :y 0
                 :width 1000
                 :height 1000
                 :fill "url(#hatching-sanguine-secondary)"}]])]]))

(def tenne
  (let [id "hatching-tenne"]
    [id
     [:<>
      (let [width (* spacing 2)]
        [:pattern {:id "hatching-tenne-secondary"
                   :width width
                   :height width
                   :pattern-units "userSpaceOnUse"}
         [:g {:stroke "#000"
              :stroke-width line-thickness}
          [:path {:d (str "M" 0 "," 0 " L" 0 "," width)}]
          [:path {:d (str "M" spacing "," 0 " L" spacing "," width)}]
          [:path {:d (str "M" width "," 0 " L" width "," width)}]]])
      (let [width 150
            height 200]
        [:pattern {:id id
                   :width width
                   :height height
                   :pattern-units "userSpaceOnUse"}
         [:defs]

         [:rect {:x 0
                 :y 0
                 :width width
                 :height height
                 :fill "#f5f5f5"}]
         [:rect {:x 0
                 :y 0
                 :width 1000
                 :height 1000
                 :fill "url(#hatching-vert)"}]
         [:rect {:x 0
                 :y 0
                 :width 1000
                 :height 1000
                 :fill "url(#hatching-tenne-secondary)"}]])]]))

(def tinctures
  {;; metal
   :argent argent
   :or or
   ;; tincture
   :azure azure
   :vert vert
   :gules gules
   :sable sable
   :purpure purpure
   ;; stain
   :murrey murrey
   :sanguine sanguine
   :tenne tenne
   ;; secondary
   :white argent})

(def patterns
  (into
   [:defs]
   (for [[_ pattern] (vals tinctures)]
     pattern)))

(defn get-for [tincture]
  (let [[id _] (get tinctures tincture)]
    (if id
      (str "url(#" id ")")
      "#888")))
