(ns heraldry.coat-of-arms.ordinary.type.bordure
  (:require
   [heraldry.coat-of-arms.field.environment :as environment]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/bordure)

(defmethod ordinary-interface/display-name ordinary-type [_] {:en "Bordure"
                                                              :de "Schildbord"})

(defmethod interface/options ordinary-type [_context]
  {:geometry {:size {:type :range
                     :min 0.1
                     :max 35
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
        width (:width environment)
        band-width ((util/percent-of width) size)
        environment-shape (environment/effective-shape environment)
        bordure-shape (environment/shrink-shape environment-shape band-width)
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
