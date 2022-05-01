(ns heraldicon.heraldry.field.type.tierced-per-fess
  (:require
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.render.outline :as outline]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/tierced-per-fess)

(defmethod field.interface/display-name field-type [_] :string.field.type/tierced-per-fess)

(defmethod field.interface/part-names field-type [_] ["chief" "fess" "base"])

(defmethod interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line)
                                 :fimbriation? false)]
    {:anchor {:point {:type :choice
                      :choices [[:string.option.point-choice/fess :fess]
                                [:string.option.point-choice/chief :chief]
                                [:string.option.point-choice/base :base]
                                [:string.option.point-choice/honour :honour]
                                [:string.option.point-choice/nombril :nombril]
                                [:string.option.point-choice/top :top]
                                [:string.option.point-choice/bottom :bottom]]
                      :default :fess
                      :ui {:label :string.option/point}}
              :offset-y {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label :string.option/offset-y
                              :step 0.1}}
              :ui {:label :string.option/anchor
                   :form-type :position}}
     :layout {:stretch-y {:type :range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui {:label :string.option/stretch-y
                               :step 0.01}}
              :ui {:label :string.option/layout
                   :form-type :field-layout}}
     :line line-style}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        anchor-point (position/calculate anchor environment :fess)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        left (assoc (:left points) :y (:y anchor-point))
        right (assoc (:right points) :y (:y anchor-point))
        height (:height environment)
        middle-half-height (-> height
                               (/ 6)
                               (* stretch-y))
        row1 (- (:y anchor-point) middle-half-height)
        row2 (+ (:y anchor-point) middle-half-height)
        [first-left first-right] (v/environment-intersections
                                  (v/v (:x left) row1)
                                  (v/v (:x right) row1)
                                  environment)
        [second-left second-right] (v/environment-intersections
                                    (v/v (:x left) row2)
                                    (v/v (:x right) row2)
                                    environment)
        shared-start-x (- (min (:x first-left)
                               (:x second-left))
                          30)
        real-start (min (-> first-left :x (- shared-start-x))
                        (-> second-left :x (- shared-start-x)))
        real-end (max (-> first-right :x (- shared-start-x))
                      (-> second-right :x (- shared-start-x)))
        shared-end-x (+ real-end 30)
        first-left (v/v shared-start-x (:y first-left))
        second-left (v/v shared-start-x (:y second-left))
        first-right (v/v shared-end-x (:y first-right))
        second-right (v/v shared-end-x (:y second-right))
        {line-one :line
         line-one-start :line-start} (line/create line
                                                  first-left first-right
                                                  :real-start real-start
                                                  :real-end real-end
                                                  :context context
                                                  :environment environment)
        {line-reversed :line
         line-reversed-start :line-start} (line/create line
                                                       second-left second-right
                                                       :reversed? true
                                                       :flipped? true
                                                       :mirrored? true
                                                       :real-start real-start
                                                       :real-end real-end
                                                       :context context
                                                       :environment environment)
        parts [[["M" (v/add first-left
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [(v/add first-right
                                        line-one-start)
                                 (v/add first-left
                                        line-one-start)])
                 "z"]
                [top-left
                 (v/v (:x right)
                      (:y first-right))]]

               [["M" (v/add first-left
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :clockwise
                                [:right :right]
                                [(v/add first-left
                                        line-one-start)
                                 (v/add second-right
                                        line-reversed-start)])
                 (path/stitch line-reversed)
                 (infinity/path :clockwise
                                [:left :left]
                                [(v/add second-left
                                        line-reversed-start)
                                 (v/add first-left
                                        line-one-start)])
                 "z"]
                [(v/v (:x left)
                      (:y first-left))
                 (v/v (:x right)
                      (:y second-right))]]

               [["M" (v/add second-right
                            line-reversed-start)
                 (path/stitch line-reversed)
                 (infinity/path :counter-clockwise
                                [:left :right]
                                [(v/add second-left
                                        line-reversed-start)
                                 (v/add second-right
                                        line-reversed-start)])
                 "z"]
                [(v/v (:x left)
                      (:y second-left))
                 bottom-right]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all
       [(path/make-path
         ["M" (v/add second-right
                     line-reversed-start)
          (path/stitch line-reversed)])]
       nil]
      environment]
     (when outline?
       [:g (outline/style context)
        [:path {:d (path/make-path
                    ["M" (v/add first-left
                                line-one-start)
                     (path/stitch line-one)])}]
        [:path {:d (path/make-path
                    ["M" (v/add second-right
                                line-reversed-start)
                     (path/stitch line-reversed)])}]])]))
