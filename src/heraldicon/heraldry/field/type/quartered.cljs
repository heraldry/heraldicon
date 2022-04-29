(ns heraldicon.heraldry.field.type.quartered
  (:require
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.heraldry.infinity :as infinity]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.outline :as outline]
   [heraldicon.heraldry.position :as position]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/quartered)

(defmethod field.interface/display-name field-type [_] :string.field.type/quartered)

(defmethod field.interface/part-names field-type [_] ["I" "II" "III" "IV"])

(defmethod interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line)
                                     :fimbriation? false)
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line)
                                              :fimbriation? false
                                              :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))]
    {:anchor {:point {:type :choice
                      :choices [[:string.option.point-choice/chief :chief]
                                [:string.option.point-choice/base :base]
                                [:string.option.point-choice/fess :fess]
                                [:string.option.point-choice/dexter :dexter]
                                [:string.option.point-choice/sinister :sinister]
                                [:string.option.point-choice/honour :honour]
                                [:string.option.point-choice/nombril :nombril]]
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
     :line line-style
     :opposite-line opposite-line-style
     :outline? options/plain-outline?-option}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        anchor-point (position/calculate anchor environment :fess)
        top (assoc (:top points) :x (:x anchor-point))
        top-left (:top-left points)
        top-right (:top-right points)
        bottom (assoc (:bottom points) :x (:x anchor-point))
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        left (assoc (:left points) :y (:y anchor-point))
        right (assoc (:right points) :y (:y anchor-point))
        intersection-top (v/find-first-intersection-of-ray anchor-point top environment)
        intersection-bottom (v/find-first-intersection-of-ray anchor-point bottom environment)
        intersection-left (v/find-first-intersection-of-ray anchor-point left environment)
        intersection-right (v/find-first-intersection-of-ray anchor-point right environment)
        arm-length (->> [intersection-top
                         intersection-bottom
                         intersection-left
                         intersection-right]
                        (map #(-> %
                                  (v/sub anchor-point)
                                  v/abs))
                        (apply max))
        full-arm-length (+ arm-length 30)
        point-top (-> (v/v 0 -1)
                      (v/mul full-arm-length)
                      (v/add anchor-point))
        point-bottom (-> (v/v 0 1)
                         (v/mul full-arm-length)
                         (v/add anchor-point))
        point-left (-> (v/v -1 0)
                       (v/mul full-arm-length)
                       (v/add anchor-point))
        point-right (-> (v/v 1 0)
                        (v/mul full-arm-length)
                        (v/add anchor-point))
        line line
        {line-top :line
         line-top-start :line-start} (line/create line
                                                  anchor-point point-top
                                                  :reversed? true
                                                  :real-start 0
                                                  :real-end arm-length
                                                  :context context
                                                  :environment environment)
        {line-right :line
         line-right-start :line-start} (line/create opposite-line
                                                    anchor-point point-right
                                                    :flipped? true
                                                    :mirrored? true
                                                    :real-start 0
                                                    :real-end arm-length
                                                    :context context
                                                    :environment environment)
        {line-bottom :line
         line-bottom-start :line-start} (line/create line
                                                     anchor-point point-bottom
                                                     :reversed? true
                                                     :real-start 0
                                                     :real-end arm-length
                                                     :context context
                                                     :environment environment)
        {line-left :line
         line-left-start :line-start} (line/create opposite-line
                                                   anchor-point point-left
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
                 (path/stitch line-left)
                 (infinity/path :clockwise
                                [:left :top]
                                [(v/add point-left
                                        line-left-start)
                                 (v/add point-top
                                        line-top-start)])
                 "z"]
                [top-left anchor-point]]

               [["M" (v/add point-top
                            line-top-start)
                 (path/stitch line-top)
                 "L" anchor-point
                 (path/stitch line-right)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [(v/add point-right
                                        line-right-start)
                                 (v/add point-top
                                        line-top-start)])
                 "z"]
                [anchor-point top-right]]

               [["M" (v/add point-bottom
                            line-bottom-start)
                 (path/stitch line-bottom)
                 "L" anchor-point
                 (path/stitch line-left)
                 (infinity/path :counter-clockwise
                                [:left :bottom]
                                [(v/add point-left
                                        line-left-start)
                                 (v/add point-bottom
                                        line-bottom-start)])
                 "z"]
                [anchor-point bottom-left]]

               [["M" (v/add point-bottom
                            line-bottom-start)
                 (path/stitch line-bottom)
                 "L" anchor-point
                 (path/stitch line-right)
                 (infinity/path :clockwise
                                [:right :bottom]
                                [(v/add point-right
                                        line-right-start)
                                 (v/add point-bottom
                                        line-bottom-start)])
                 "z"]
                [anchor-point bottom-right]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all
       [(path/make-path
         ["M" anchor-point
          (path/stitch line-right)])]
       [(path/make-path
         ["M" (v/add point-bottom
                     line-bottom-start)
          (path/stitch line-bottom)])]
       nil]
      environment]
     (when outline?
       [:g (outline/style context)
        [:path {:d (path/make-path
                    ["M" (v/add point-top
                                line-top-start)
                     (path/stitch line-top)])}]
        [:path {:d (path/make-path
                    ["M" anchor-point
                     (path/stitch line-right)])}]
        [:path {:d (path/make-path
                    ["M" (v/add point-bottom
                                line-bottom-start)
                     (path/stitch line-bottom)])}]
        [:path {:d (path/make-path
                    ["M" anchor-point
                     (path/stitch line-left)])}]])]))
