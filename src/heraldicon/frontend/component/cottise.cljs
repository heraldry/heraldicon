(ns heraldicon.frontend.component.cottise
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.interface :as ui.interface]
   [heraldicon.frontend.validation :as validation]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]))

(defn- form [context]
  (ui.interface/form-elements
   context
   [:line
    :opposite-line
    :distance
    :thickness
    :outline?]))

;; TODO: context
(defn cottise-name [{:keys [path] :as context}]
  (let [cottise-key (last path)
        ordinary-type (interface/get-raw-data (-> context
                                                  (c/-- 2)
                                                  (c/++ :type)))
        names (cond
                (#{:heraldry.ordinary.type/pale}
                 ordinary-type) {:cottise-1 (string/str-tr "1 " :string.miscellaneous/left)
                                 :cottise-2 (string/str-tr "2 " :string.miscellaneous/left)
                                 :cottise-opposite-1 (string/str-tr "1 " :string.miscellaneous/right)
                                 :cottise-opposite-2 (string/str-tr "2 " :string.miscellaneous/right)}
                (#{:heraldry.ordinary.type/fess
                   :heraldry.ordinary.type/bend
                   :heraldry.ordinary.type/bend-sinister
                   :heraldry.ordinary.type/chevron}
                 ordinary-type) {:cottise-1 (string/str-tr "1 " :string.miscellaneous/top)
                                 :cottise-2 (string/str-tr "2 " :string.miscellaneous/top)
                                 :cottise-opposite-1 (string/str-tr "1 " :string.miscellaneous/bottom)
                                 :cottise-opposite-2 (string/str-tr "2 " :string.miscellaneous/bottom)}
                :else {:cottise-1 "1"
                       :cottise-2 "2"
                       :cottise-opposite-1 (string/str-tr "1 " :string.miscellaneous/opposite)
                       :cottise-opposite-2 (string/str-tr "2 " :string.miscellaneous/opposite)
                       :cottise-extra-1 (string/str-tr "1 " :string.miscellaneous/extra)
                       :cottise-extra-2 (string/str-tr "2 " :string.miscellaneous/extra)})]
    (string/str-tr
     :string.entity/cottise
     " "
     (get names cottise-key))))

(defmethod ui.interface/component-node-data :heraldry/cottise [context]
  {:title (cottise-name context)
   :validation (validation/validate-cottise context)
   :nodes [{:context (c/++ context :field)}]})

(defmethod ui.interface/component-form-data :heraldry/cottise [_context]
  {:form form})
