(ns heraldry.coat-of-arms.ordinary.type.bordure
  (:require
   ["svgpath" :as svgpath]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.strings :as strings]))

(def ordinary-type :heraldry.ordinary.type/bordure)

(defmethod ordinary-interface/display-name ordinary-type [_] {:en "Bordure"
                                                              :de "Schildbord"})

(defmethod interface/options ordinary-type [_context]
  {:geometry {:size {:type :range
                     :min 0.1
                     :max 90
                     :default 10
                     :ui {:label strings/size
                          :step 0.1}}
              :ui {:label strings/geometry
                   :form-type :geometry}}
   :outline? options/plain-outline?-option})

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [size (interface/get-sanitized-data (c/++ context :geometry :size))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        middle (-> top-left
                   (v/add bottom-right)
                   (v/div 2))
        environment-shape (-> environment :shape :paths first)
        bordure-shape (-> environment-shape
                          svgpath
                          (.translate
                           (- (:x middle))
                           (- (:y middle)))
                          (.scale (- 1.0 (/ size 100)))
                          (.translate
                           (:x middle)
                           (:y middle))
                          .toString)
        part [{:paths [environment-shape
                       bordure-shape]}
              [(:top-left points)
               (:bottom-right points)]]]
    [:<>
     [field-shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (when outline?
       [:g (outline/style context)
        [:path {:d bordure-shape}]])]))
