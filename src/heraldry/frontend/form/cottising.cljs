(ns heraldry.frontend.form.cottising
  (:require [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.line :as line]
            [heraldry.frontend.form.state]
            [heraldry.frontend.form.tincture :as tincture]
            [heraldry.frontend.util :as util]
            [heraldry.util :refer [id]]
            [re-frame.core :as rf]))

(defn form-for-single-cottise [path options & {:keys [title] :or {title "Cottise"}}]
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
         (when (-> options :tincture)
           [tincture/form (conj path :tincture)
            :label "Tincture"])]
        [:span.disabled "Disabled"])]]))

(defn form [path options & {:keys [title] :or {title "Cottising"}}]
  (let [current-data @(rf/subscribe [:get path])
        link-name    "TBD"]
    [:div.setting
     [:label title]
     " "
     [element/submenu path "Cottising" link-name {}
      (when (:cottise-1 options)
        [form-for-single-cottise (conj path :cottise-1) (:cottise-1 options)
         :title "Cottise 1"])
      (when (and (:cottise-2 options)
                 (-> current-data :cottise-1 :enabled?))
        [form-for-single-cottise (conj path :cottise-2) (:cottise-2 options)
         :title "Cottise 2"])
      (when (:cottise-opposite-1 options)
        [form-for-single-cottise (conj path :cottise-opposite-1) (:cottise-opposite-1 options)
         :title "Cottise 1 Opp"])
      (when (and (:cottise-opposite-2 options)
                 (-> current-data :cottise-opposite-1 :enabled?))
        [form-for-single-cottise (conj path :cottise-opposite-2) (:cottise-opposite-2 options)
         :title "Cottise 2 Opp"])]]))

