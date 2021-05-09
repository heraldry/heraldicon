(ns heraldry.frontend.form.cottising
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.line :as line]
            heraldry.frontend.form.state
            [heraldry.util :refer [id]]
            [re-frame.core :as rf]))

(defn form-for-single-cottise [path options & {:keys [title form-for-field] :or {title "Cottise"}}]
  (let [cottise     @(rf/subscribe [:get path])
        checkbox-id (id "checkbox")
        enabled?    (:enabled? cottise)]
    [:div.setting
     [:label title]
     " "
     [:div.other
      [:input {:type      "checkbox"
               :id        checkbox-id
               :checked   enabled?
               :on-change #(let [new-checked? (-> % .-target .-checked)]
                             (when new-checked?
                               (rf/dispatch-sync [:set (conj path :line :type) :straight])
                               (rf/dispatch-sync [:set (conj path :field) default/field]))
                             (rf/dispatch [:set (conj path :enabled?) new-checked?]))}]
      (if enabled?
        [element/submenu path title "Cottise" {}
         (when (:line options)
           [line/form (conj path :line) :options (:line options)])
         (when (:opposite-line options)
           [line/form (conj path :opposite-line)
            :options (:opposite-line options)
            :defaults (options/sanitize (:line cottise) (:line options))
            :title "Opposite Line"])
         (when (-> options :distance)
           [element/range-input (conj path :distance)
            "Distance"
            (-> options :distance :min)
            (-> options :distance :max)
            :default (-> options :distance :default)
            :step 0.1
            :display-function #(str % "%")])
         (when (-> options :thickness)
           [element/range-input (conj path :thickness)
            "Thickness"
            (-> options :thickness :min)
            (-> options :thickness :max)
            :default (-> options :thickness :default)
            :step 0.1
            :display-function #(str % "%")])
         (when (-> options :field)
           [form-for-field (conj path :field)
            :title-prefix "Field"])]
        [:span.disabled "Disabled"])]]))

(defn form [path options & {:keys [title form-for-field] :or {title "Cottising"}}]
  (let [current-data @(rf/subscribe [:get path])
        link-name    (cond
                       (or (and (-> current-data :cottise-1 :enabled?)
                                (:cottise-1 options)
                                (-> current-data :cottise-2 :enabled?)
                                (:cottise-2 options))
                           (and (-> current-data :cottise-opposite-1 :enabled?)
                                (:cottise-opposite-1 options)
                                (-> current-data :cottise-opposite-2 :enabled?)
                                (:cottise-opposite-2 options))) "Double"
                       (or (and (-> current-data :cottise-1 :enabled?)
                                (:cottise-1 options))
                           (and (-> current-data :cottise-opposite-1 :enabled?)
                                (:cottise-opposite-1 options))) "Single"
                       :else                                    "None")]
    [:div.setting
     [:label title]
     " "
     [element/submenu path "Cottising" link-name {}
      (when (:cottise-1 options)
        [form-for-single-cottise (conj path :cottise-1) (:cottise-1 options)
         :title "Cottise 1"
         :form-for-field form-for-field])
      (when (and (:cottise-2 options)
                 (-> current-data :cottise-1 :enabled?))
        [form-for-single-cottise (conj path :cottise-2) (:cottise-2 options)
         :title "Cottise 2"
         :form-for-field form-for-field])
      (when (:cottise-opposite-1 options)
        [form-for-single-cottise (conj path :cottise-opposite-1) (:cottise-opposite-1 options)
         :title "Cottise 1 Opp"
         :form-for-field form-for-field])
      (when (and (:cottise-opposite-2 options)
                 (-> current-data :cottise-opposite-1 :enabled?))
        [form-for-single-cottise (conj path :cottise-opposite-2) (:cottise-opposite-2 options)
         :title "Cottise 2 Opp"
         :form-for-field form-for-field])]]))

