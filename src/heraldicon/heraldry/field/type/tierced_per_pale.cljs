(ns heraldicon.heraldry.field.type.tierced-per-pale
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/tierced-per-pale)

(defmethod field.interface/display-name field-type [_] :string.field.type/tierced-per-pale)

(defmethod field.interface/part-names field-type [_] ["dexter" "fess" "sinister"])

(defmethod field.interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line)
                                 :fimbriation? false)]
    {:anchor {:point {:type :choice
                      :choices (position/anchor-choices
                                [:fess
                                 :dexter
                                 :sinister
                                 :hoist
                                 :fly
                                 :left
                                 :center
                                 :right])
                      :default :fess
                      :ui/label :string.option/point}
              :offset-x {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui/label :string.option/offset-x
                         :ui/step 0.1}
              :ui/label :string.option/anchor
              :ui/element :ui.element/position}
     :layout {:stretch-x {:type :range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui/label :string.option/stretch-x
                          :ui/step 0.01}
              :ui/label :string.option/layout
              :ui/element :ui.element/field-layout}
     :line line-style}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        anchor-point (position/calculate anchor environment :fess)
        top (assoc (:top points) :x (:x anchor-point))
        top-left (:top-left points)
        bottom (assoc (:bottom points) :x (:x anchor-point))
        bottom-right (:bottom-right points)
        width (:width environment)
        middle-half-width (-> width
                              (/ 6)
                              (* stretch-x))
        col1 (- (:x anchor-point) middle-half-width)
        col2 (+ (:x anchor-point) middle-half-width)
        [first-top first-bottom] (v/environment-intersections
                                  (v/Vector. col1 (:y top))
                                  (v/Vector. col1 (:y bottom))
                                  environment)
        [second-top second-bottom] (v/environment-intersections
                                    (v/Vector. col2 (:y top))
                                    (v/Vector. col2 (:y bottom))
                                    environment)
        shared-start-y (- (min (:y first-top)
                               (:y second-top))
                          30)
        real-start (min (-> first-top :y (- shared-start-y))
                        (-> second-top :y (- shared-start-y)))
        real-end (max (-> first-bottom :y (- shared-start-y))
                      (-> second-bottom :y (- shared-start-y)))
        shared-end-y (+ real-end 30)
        first-top (v/Vector. (:x first-top) shared-start-y)
        second-top (v/Vector. (:x second-top) shared-start-y)
        first-bottom (v/Vector. (:x first-bottom) shared-end-y)
        second-bottom (v/Vector. (:x second-bottom) shared-end-y)
        {line-one :line
         line-one-start :line-start} (line/create line
                                                  first-top first-bottom
                                                  :real-start real-start
                                                  :real-end real-end
                                                  :context context
                                                  :environment environment)
        {line-reversed :line
         line-reversed-start :line-start} (line/create line
                                                       second-top second-bottom
                                                       :reversed? true
                                                       :flipped? true
                                                       :mirrored? true
                                                       :real-start real-start
                                                       :real-end real-end
                                                       :context context
                                                       :environment environment)
        parts [[["M" (v/add first-top
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :clockwise
                                [:bottom :top]
                                [(v/add first-bottom
                                        line-one-start)
                                 (v/add first-top
                                        line-one-start)])
                 "z"]
                [top-left
                 (v/Vector. (:x first-bottom)
                            (:y bottom))]]

               [["M" (v/add second-bottom
                            line-reversed-start)
                 (path/stitch line-reversed)
                 (infinity/path :counter-clockwise
                                [:top :top]
                                [(v/add second-top
                                        line-reversed-start)
                                 (v/add first-top
                                        line-one-start)])
                 (path/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:bottom :bottom]
                                [(v/add first-top
                                        line-one-start)
                                 (v/add second-bottom
                                        line-reversed-start)
                                 first-bottom second-bottom])
                 "z"]
                [(v/Vector. (:x first-top)
                            (:y top))
                 (v/Vector. (:x second-bottom)
                            (:y bottom))]]

               [["M" (v/add second-bottom
                            line-reversed-start)
                 (path/stitch line-reversed)
                 (infinity/path :clockwise
                                [:top :bottom]
                                [(v/add second-top
                                        line-reversed-start)
                                 (v/add second-bottom
                                        line-reversed-start)])
                 "z"]
                [(v/Vector. (:x second-top)
                            (:y top))
                 bottom-right]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all
       [(path/make-path
         ["M" (v/add second-bottom
                     line-reversed-start)
          (path/stitch line-reversed)])]
       nil]
      environment]
     (when outline?
       [:g (outline/style context)
        [:path {:d (path/make-path
                    ["M" (v/add first-top
                                line-one-start)
                     (path/stitch line-one)])}]
        [:path {:d (path/make-path
                    ["M" (v/add second-bottom
                                line-reversed-start)
                     (path/stitch line-reversed)])}]])]))
