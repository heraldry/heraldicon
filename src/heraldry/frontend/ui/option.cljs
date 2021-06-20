(ns heraldry.frontend.ui.option
  (:require [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.escutcheon :as escutcheon]
            [heraldry.frontend.ui.element :as ui-element]
            [heraldry.coat-of-arms.texture :as texture]
            [heraldry.frontend.form.theme :as theme]))

(defn default-element [type]
  (case type
    :choice :select
    :boolean :checkbox
    :range :range
    nil))

(defmulti form (fn [_path {:keys [type ui]}]
                 (or (:form-type ui)
                     (default-element type))))

(defmethod form nil [_path option]
  (when option
    [:div (str "not implemented: " (:type option) (-> option :ui :form-type))]))

(defmethod form :select [path {:keys [ui default choices] :as option}]
  (when option
    [ui-element/select path choices
     :default default
     :label (:label ui)]))

(defmethod form :radio-select [path {:keys [ui default choices] :as option}]
  (when option
    [ui-element/radio-select path choices
     :default default
     :label (:label ui)]))

(defmethod form :checkbox [path {:keys [ui default] :as option}]
  (when option
    [ui-element/checkbox path
     :default default
     :label (:label ui)]))

(defmethod form :escutcheon [path {:keys [ui default choices] :as option}]
  (when option
    [escutcheon/ui-form path choices
     :default default
     :label (:label ui)]))

(defmethod form :theme [path {:keys [ui default choices] :as option}]
  (when option
    [theme/ui-form path choices
     :default default
     :label (:label ui)]))
