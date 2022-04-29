(ns heraldicon.coat-of-arms.field.type.tierced-per-pale
  (:require
   [heraldicon.coat-of-arms.field.interface :as field.interface]
   [heraldicon.coat-of-arms.field.shared :as shared]
   [heraldicon.coat-of-arms.infinity :as infinity]
   [heraldicon.coat-of-arms.line.core :as line]
   [heraldicon.coat-of-arms.outline :as outline]
   [heraldicon.coat-of-arms.position :as position]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.math.svg.path :as path]
   [heraldicon.math.vector :as v]))

(def field-type :heraldry.field.type/tierced-per-pale)

(defmethod field.interface/display-name field-type [_] :string.field.type/tierced-per-pale)

(defmethod field.interface/part-names field-type [_] ["dexter" "fess" "sinister"])

(defmethod interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line)
                                 :fimbriation? false)]
    {:anchor {:point {:type :choice
                      :choices [[:string.option.point-choice/fess :fess]
                                [:string.option.point-choice/dexter :dexter]
                                [:string.option.point-choice/sinister :sinister]
                                [:string.option.point-choice/left :left]
                                [:string.option.point-choice/right :right]]
                      :default :fess
                      :ui {:label :string.option/point}}
              :offset-x {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label :string.option/offset-x
                              :step 0.1}}
              :ui {:label :string.option/anchor
                   :form-type :position}}
     :layout {:stretch-x {:type :range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui {:label :string.option/stretch-x
                               :step 0.01}}
              :ui {:label :string.option/layout
                   :form-type :field-layout}}
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
                                  (v/v col1 (:y top))
                                  (v/v col1 (:y bottom))
                                  environment)
        [second-top second-bottom] (v/environment-intersections
                                    (v/v col2 (:y top))
                                    (v/v col2 (:y bottom))
                                    environment)
        shared-start-y (- (min (:y first-top)
                               (:y second-top))
                          30)
        real-start (min (-> first-top :y (- shared-start-y))
                        (-> second-top :y (- shared-start-y)))
        real-end (max (-> first-bottom :y (- shared-start-y))
                      (-> second-bottom :y (- shared-start-y)))
        shared-end-y (+ real-end 30)
        first-top (v/v (:x first-top) shared-start-y)
        second-top (v/v (:x second-top) shared-start-y)
        first-bottom (v/v (:x first-bottom) shared-end-y)
        second-bottom (v/v (:x second-bottom) shared-end-y)
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
                 (v/v (:x first-bottom)
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
                [(v/v (:x first-top)
                      (:y top))
                 (v/v (:x second-bottom)
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
                [(v/v (:x second-top)
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
