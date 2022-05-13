(ns heraldicon.heraldry.field.type.chevronny
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.shared.chevron :as chevron]
   [heraldicon.interface :as interface]
   [heraldicon.math.angle :as angle]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/chevronny)

(defmethod field.interface/display-name field-type [_] :string.field.type/chevronny)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line)
                                     :fimbriation? false)
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line)
                                              :fimbriation? false
                                              :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))
        orientation-point-option {:type :choice
                                  :choices [[:string.option.point-choice/top-left :top-left]
                                            [:string.option.point-choice/top :top]
                                            [:string.option.point-choice/top-right :top-right]
                                            [:string.option.point-choice/left :left]
                                            [:string.option.point-choice/right :right]
                                            [:string.option.point-choice/bottom-left :bottom-left]
                                            [:string.option.point-choice/bottom :bottom]
                                            [:string.option.point-choice/bottom-right :bottom-right]
                                            [:string.option.point-choice/fess :fess]
                                            [:string.option.point-choice/chief :chief]
                                            [:string.option.point-choice/base :base]
                                            [:string.option.point-choice/dexter :dexter]
                                            [:string.option.point-choice/sinister :sinister]
                                            [:string.option.point-choice/honour :honour]
                                            [:string.option.point-choice/nombril :nombril]
                                            [:string.option.orientation-point-choice/angle :angle]]
                                  :default :angle
                                  :ui {:label :string.option/point}}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    {:orientation (cond-> {:point orientation-point-option
                           :ui {:label :string.option/orientation
                                :form-type :position}}

                    (= current-orientation-point
                       :angle) (assoc :angle {:type :range
                                              :min 10
                                              :max 170
                                              :default 45
                                              :ui {:label :string.option/angle}})

                    (not= current-orientation-point
                          :angle) (assoc :offset-x {:type :range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui {:label :string.option/offset-x
                                                         :step 0.1}}
                                         :offset-y {:type :range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui {:label :string.option/offset-y
                                                         :step 0.1}}))
     :layout {:num-fields-y {:type :range
                             :min 1
                             :max 20
                             :default 6
                             :integer? true
                             :ui {:label :string.option/subfields-y
                                  :form-type :field-layout-num-fields-y}}
              :num-base-fields {:type :range
                                :min 2
                                :max 8
                                :default 2
                                :integer? true
                                :ui {:label :string.option/base-fields
                                     :form-type :field-layout-num-base-fields}}
              :offset-y {:type :range
                         :min -3
                         :max 3
                         :default 0
                         :ui {:label :string.option/offset-y
                              :step 0.01}}
              :stretch-y {:type :range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui {:label :string.option/stretch-y
                               :step 0.01}}
              :ui {:label :string.option/layout
                   :form-type :field-layout}}
     :line line-style
     :opposite-line opposite-line-style}))

(defn chevronny-parts [top-left bottom-right line opposite-line outline? context]
  (let [environment (:environment context)
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        height (- (:y bottom-right)
                  (:y top-left))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        chevron-angle 90
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               environment
                                               {:point :fess
                                                :offset-x 0
                                                :offset-y 0}
                                               orientation
                                               0 ;; ignored, since there's no alignment
                                               chevron-angle)
        [relative-left relative-right] (chevron/arm-diagonals chevron-angle anchor-point orientation-point)
        diagonal-left (v/add anchor-point relative-left)
        diagonal-right (v/add anchor-point relative-right)
        angle-left (angle/normalize (v/angle-to-point anchor-point diagonal-left))
        angle-right (angle/normalize (v/angle-to-point anchor-point diagonal-right))
        joint-angle (angle/normalize (- angle-left angle-right))
        chevron-middle-height (-> height
                                  (/ (dec num-fields-y))
                                  (* stretch-y))
        required-height (* chevron-middle-height
                           (dec num-fields-y))
        middle (-> height
                   (/ 2)
                   (+ (:y top-left)))
        y0 (-> middle
               (- (/ required-height 2))
               (+ (* offset-y
                     chevron-middle-height)))
        x (:x anchor-point)
        left-x (:x top-left)
        right-x (:x bottom-right)
        start v/zero
        end-left (-> (v/Vector. (* 2 height) 0)
                     (v/rotate (+ 90 (/ joint-angle 2))))
        end-right (-> (v/Vector. (* 2 height) 0)
                      (v/rotate (- 90 (/ joint-angle 2))))
        {line-left-upper :line
         line-left-upper-start :line-start} (line/create line
                                                         start end-left
                                                         :reversed? true
                                                         :context context
                                                         :environment environment)

        {line-right-upper :line} (line/create opposite-line
                                              start end-right
                                              :context context
                                              :environment environment)
        {line-left-lower :line} (line/create line
                                             start end-left
                                             :flipped? true
                                             :mirrored? true
                                             :context context
                                             :environment environment)
        {line-right-lower :line
         line-right-lower-start :line-start} (line/create opposite-line
                                                          start end-right
                                                          :reversed? true
                                                          :context context
                                                          :environment environment)
        parts (->> (range num-fields-y)
                   (map (fn [i]
                          (let [y1 (+ y0 (* (dec i) chevron-middle-height))
                                y2 (+ y1 chevron-middle-height)
                                last-part? (-> i inc (= num-fields-y))
                                line-corner-upper (v/Vector. x y1)
                                line-corner-lower (v/Vector. x y2)]
                            [(cond
                               (and (zero? i)
                                    last-part?) ["M" -1000 -1000
                                                 "h" 2000
                                                 "v" 2000
                                                 "h" -2000
                                                 "z"]
                               (zero? i) ["M" line-corner-lower
                                          (path/stitch line-right-upper)
                                          (infinity/path :counter-clockwise
                                                         [:right :left]
                                                         [(v/add line-corner-lower
                                                                 end-right)
                                                          (v/add line-corner-lower
                                                                 end-left
                                                                 line-left-upper-start)])
                                          (path/stitch line-left-upper)]
                               :else (concat ["M" line-corner-upper
                                              (path/stitch line-right-upper)]
                                             (cond
                                               last-part? [(infinity/path :clockwise
                                                                          [:right :left]
                                                                          [(v/add line-corner-upper
                                                                                  end-right)
                                                                           (v/add line-corner-upper
                                                                                  end-left
                                                                                  line-left-upper-start)])
                                                           (path/stitch line-left-upper)]
                                               :else [(infinity/path :clockwise
                                                                     [:right :right]
                                                                     [(v/add line-corner-upper
                                                                             end-right)
                                                                      (v/add line-corner-lower
                                                                             end-right
                                                                             line-right-lower-start)])
                                                      (path/stitch line-right-lower)
                                                      (path/stitch line-left-lower)
                                                      (infinity/path :clockwise
                                                                     [:left :left]
                                                                     [(v/add line-corner-lower
                                                                             end-left)
                                                                      (v/add line-corner-upper
                                                                             end-left
                                                                             line-left-upper-start)])
                                                      (path/stitch line-left-upper)])))
                             [(assoc line-corner-upper :x left-x)
                              (assoc line-corner-lower :x right-x)]])))
                   vec)
        edges (->> num-fields-y
                   dec
                   range
                   (map (fn [i]
                          (let [y1 (+ y0 (* i chevron-middle-height))
                                line-corner-lower (v/Vector. x y1)]
                            (path/make-path ["M" (v/add line-corner-lower
                                                        end-left
                                                        line-left-upper-start)
                                             (path/stitch line-left-upper)
                                             (path/stitch line-right-upper)]))))
                   vec)
        overlap (-> (mapv vector edges)
                    (conj nil))
        outlines (when outline?
                   [:g (outline/style context)
                    (for [i (range (dec num-fields-y))]
                      ^{:key i}
                      [:path {:d (nth edges i)}])])]
    [parts overlap outlines]))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        [parts overlap outlines] (chevronny-parts top-left bottom-right line opposite-line outline? context)]
    [:<>
     [shared/make-subfields
      context parts
      overlap
      environment]
     outlines]))
