(ns heraldicon.heraldry.field.type.gyronny
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.shared.saltire :as saltire]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/gyronny)

(defmethod field.interface/display-name field-type [_] :string.field.type/gyronny)

(defmethod field.interface/part-names field-type [_] ["I" "II" "III" "IV" "V" "VI" "VII" "VIII"])

(defmethod field.interface/options field-type [context]
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
                                  :choices (position/orientation-choices
                                            [:top-left
                                             :top-right
                                             :bottom-left
                                             :bottom-right
                                             :angle])
                                  :default :top-left
                                  :ui {:label :string.option/point}}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    {:anchor {:point {:type :choice
                      :choices (position/anchor-choices
                                [:chief
                                 :base
                                 :fess
                                 :dexter
                                 :sinister
                                 :honour
                                 :nombril
                                 :hoist
                                 :fly
                                 :center])
                      :default :fess
                      :ui {:label :string.option/point}}
              :offset-x {:type :range
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
                              :step 0.1}}
              :ui {:label :string.option/anchor
                   :form-type :position}}
     :orientation (cond-> {:point orientation-point-option
                           :ui {:label :string.option/orientation
                                :form-type :position}}

                    (= current-orientation-point
                       :angle) (assoc :angle {:type :range
                                              :min 10
                                              :max 80
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
     :line line-style
     :opposite-line opposite-line-style}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               environment
                                               anchor
                                               orientation
                                               0
                                               nil)
        top (assoc (:top points) :x (:x anchor-point))
        bottom (assoc (:bottom points) :x (:x anchor-point))
        left (assoc (:left points) :y (:y anchor-point))
        right (assoc (:right points) :y (:y anchor-point))
        [relative-top-left relative-top-right
         relative-bottom-left relative-bottom-right] (saltire/arm-diagonals anchor-point orientation-point)
        diagonal-top-left (v/add anchor-point relative-top-left)
        diagonal-top-right (v/add anchor-point relative-top-right)
        diagonal-bottom-left (v/add anchor-point relative-bottom-left)
        diagonal-bottom-right (v/add anchor-point relative-bottom-right)
        intersection-top-left (v/find-first-intersection-of-ray anchor-point diagonal-top-left environment)
        intersection-top-right (v/find-first-intersection-of-ray anchor-point diagonal-top-right environment)
        intersection-bottom-left (v/find-first-intersection-of-ray anchor-point diagonal-bottom-left environment)
        intersection-bottom-right (v/find-first-intersection-of-ray anchor-point diagonal-bottom-right environment)
        intersection-top (v/find-first-intersection-of-ray anchor-point top environment)
        intersection-bottom (v/find-first-intersection-of-ray anchor-point bottom environment)
        intersection-left (v/find-first-intersection-of-ray anchor-point left environment)
        intersection-right (v/find-first-intersection-of-ray anchor-point right environment)
        arm-length (->> [intersection-top-left
                         intersection-top-right
                         intersection-bottom-left
                         intersection-bottom-right
                         intersection-top
                         intersection-bottom
                         intersection-left
                         intersection-right]
                        (map #(-> %
                                  (v/sub anchor-point)
                                  v/abs))
                        (apply max))
        full-arm-length (+ arm-length 30)
        point-top (-> (v/Vector. 0 -1)
                      (v/mul full-arm-length)
                      (v/add anchor-point))
        point-bottom (-> (v/Vector. 0 1)
                         (v/mul full-arm-length)
                         (v/add anchor-point))
        point-left (-> (v/Vector. -1 0)
                       (v/mul full-arm-length)
                       (v/add anchor-point))
        point-right (-> (v/Vector. 1 0)
                        (v/mul full-arm-length)
                        (v/add anchor-point))
        point-top-left diagonal-top-left
        point-top-right diagonal-top-right
        point-bottom-left diagonal-bottom-left
        point-bottom-right diagonal-bottom-right
        line (dissoc line :fimbriation)
        {line-top :line
         line-top-start :line-start} (line/create opposite-line
                                                  anchor-point point-top
                                                  :reversed? true
                                                  :real-start 0
                                                  :real-end arm-length
                                                  :context context
                                                  :environment environment)
        {line-right :line
         line-right-start :line-start} (line/create opposite-line
                                                    anchor-point point-right
                                                    :reversed? true
                                                    :real-start 0
                                                    :real-end arm-length
                                                    :context context
                                                    :environment environment)
        {line-bottom :line
         line-bottom-start :line-start} (line/create opposite-line
                                                     anchor-point point-bottom
                                                     :reversed? true
                                                     :real-start 0
                                                     :real-end arm-length
                                                     :context context
                                                     :environment environment)
        {line-left :line
         line-left-start :line-start} (line/create opposite-line
                                                   anchor-point point-left
                                                   :reversed? true
                                                   :real-start 0
                                                   :real-end arm-length
                                                   :context context
                                                   :environment environment)
        {line-top-left :line} (line/create line
                                           anchor-point point-top-left
                                           :flipped? true
                                           :mirrored? true
                                           :real-start 0
                                           :real-end arm-length
                                           :context context
                                           :environment environment)
        {line-top-right :line} (line/create line
                                            anchor-point point-top-right
                                            :flipped? true
                                            :mirrored? true
                                            :real-start 0
                                            :real-end arm-length
                                            :context context
                                            :environment environment)
        {line-bottom-right :line} (line/create line
                                               anchor-point point-bottom-right
                                               :flipped? true
                                               :mirrored? true
                                               :real-start 0
                                               :real-end arm-length
                                               :context context
                                               :environment environment)
        {line-bottom-left :line} (line/create line
                                              anchor-point point-bottom-left
                                              :flipped? true
                                              :mirrored? true
                                              :real-start 0
                                              :real-end arm-length
                                              :context context
                                              :environment environment)
        parts [[["M" (v/add point-top
                            line-top-start)
                 (path/stitch line-top)
                 "L" anchor-point
                 (path/stitch line-top-left)
                 (infinity/path :clockwise
                                [:left :top]
                                [point-top-left
                                 (v/add point-top
                                        line-top-start)])
                 "z"]
                [top-left
                 anchor-point
                 top]]

               [["M" (v/add point-top
                            line-top-start)
                 (path/stitch line-top)
                 "L" anchor-point
                 (path/stitch line-top-right)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [point-top-right
                                 (v/add point-top
                                        line-top-start)])
                 "z"]
                [top
                 anchor-point
                 top-right]]

               [["M" (v/add point-left
                            line-left-start)
                 (path/stitch line-left)
                 "L" anchor-point
                 (path/stitch line-top-left)
                 (infinity/path :counter-clockwise
                                [:left :left]
                                [point-top-left
                                 (v/add point-left
                                        line-left-start)])
                 "z"]
                [left
                 anchor-point
                 top-left]]

               [["M" (v/add point-right
                            line-right-start)
                 (path/stitch line-right)
                 "L" anchor-point
                 (path/stitch line-top-right)
                 (infinity/path :clockwise
                                [:right :right]
                                [point-top-right
                                 (v/add point-right
                                        line-right-start)])
                 "z"]
                [top-right
                 anchor-point
                 right]]

               [["M" (v/add point-left
                            line-left-start)
                 (path/stitch line-left)
                 "L" anchor-point
                 (path/stitch line-bottom-left)
                 (infinity/path :clockwise
                                [:left :left]
                                [point-bottom-left
                                 (v/add point-left
                                        line-left-start)])
                 "z"]
                [bottom-left
                 anchor-point
                 left]]

               [["M" (v/add point-right
                            line-right-start)
                 (path/stitch line-right)
                 "L" anchor-point
                 (path/stitch line-bottom-right)
                 (infinity/path :counter-clockwise
                                [:right :right]
                                [point-bottom-right
                                 (v/add point-right
                                        line-right-start)])
                 "z"]
                [right
                 anchor-point
                 bottom-right]]

               [["M" (v/add point-bottom
                            line-bottom-start)
                 (path/stitch line-bottom)
                 "L" anchor-point
                 (path/stitch line-bottom-left)
                 (infinity/path :counter-clockwise
                                [:left :bottom]
                                [point-bottom-left
                                 (v/add point-bottom
                                        line-bottom-start)])
                 "z"]
                [bottom
                 anchor-point
                 bottom-left]]

               [["M" (v/add point-bottom
                            line-bottom-start)
                 (path/stitch line-bottom)
                 "L" anchor-point
                 (path/stitch line-bottom-right)
                 (infinity/path :clockwise
                                [:right :bottom]
                                [point-bottom-right
                                 (v/add point-bottom
                                        line-bottom-start)])
                 "z"]
                [bottom-right
                 anchor-point
                 bottom]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all
       [(path/make-path
         ["M" anchor-point
          (path/stitch line-top-right)])]
       [(path/make-path
         ["M" (v/add point-left
                     line-left-start)
          (path/stitch line-left)])]
       [(path/make-path
         ["M" (v/add point-right
                     line-right-start)
          (path/stitch line-right)])]
       [(path/make-path
         ["M" anchor-point
          (path/stitch line-bottom-left)])]
       [(path/make-path
         ["M" anchor-point
          (path/stitch line-bottom-right)])]
       [(path/make-path
         ["M" (v/add point-bottom
                     line-bottom-start)
          (path/stitch line-bottom)])]
       nil]
      environment]
     (when outline?
       [:g (outline/style context)
        [:path {:d (path/make-path
                    ["M" anchor-point
                     (path/stitch line-top-left)])}]
        [:path {:d (path/make-path
                    ["M" (v/add point-top
                                line-top-start)
                     (path/stitch line-top)])}]
        [:path {:d (path/make-path
                    ["M" anchor-point
                     (path/stitch line-top-right)])}]
        [:path {:d (path/make-path
                    ["M" (v/add point-right
                                line-right-start)
                     (path/stitch line-right)])}]
        [:path {:d (path/make-path
                    ["M" anchor-point
                     (path/stitch line-bottom-right)])}]
        [:path {:d (path/make-path
                    ["M" (v/add point-bottom
                                line-bottom-start)
                     (path/stitch line-bottom)])}]
        [:path {:d (path/make-path
                    ["M" anchor-point
                     (path/stitch line-bottom-left)])}]
        [:path {:d (path/make-path
                    ["M" (v/add point-left
                                line-left-start)
                     (path/stitch line-left)])}]])]))
