(ns heraldicon.frontend.tutorial.arms
  (:require
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.element.hover-menu :as hover-menu]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.tutorial :as tutorial]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(def ^:private form-db-path
  [:forms :heraldicon.entity.type/arms])

(def ^:private achievement-path
  (conj form-db-path :data :achievement))

(def ^:private field-path
  (conj achievement-path :coat-of-arms :field))

(def ^:private ornaments-path
  (conj achievement-path :ornaments))

(defn- tour-node-selector [path]
  (str "[data-tour-path='" (tree/path->tour-id path) "']"))

(defn- tour-hover-menu-selector [path button-idx]
  (tour-node-selector (conj path button-idx)))

(def ^:private active-node-path
  [:ui :component-tree :heraldicon.frontend.library.arms.details/identifier :active-node])

(def ^:private subfield-I-path
  (conj field-path :fields 0))

(def ^:private subfield-I-field-path
  (conj field-path :fields 0 :field))

(def ^:private subfield-I-tincture-path
  (conj subfield-I-field-path :tincture))

(def ^:private subfield-I-components-path
  (conj subfield-I-field-path :components))

(def ^:private charge-path
  (conj subfield-I-components-path 0))

(def ^:private ornaments-elements-path
  (conj ornaments-path :elements))

(def goals
  [{:title :string.tutorial.arms/welcome-title
    :description :string.tutorial.arms/welcome-description}

   {:title :string.tutorial.arms/select-field-title
    :description :string.tutorial.arms/select-field-description
    :hints [{:element (tour-node-selector field-path)
             :side "left"}]
    :complete-when {:path active-node-path
                    :pred some?}}

   {:title :string.tutorial.arms/divide-field-title
    :description :string.tutorial.arms/divide-field-description
    :hints [{:element "[data-tour='field-type-select']"
             :side "left"}
            {:element "[data-tour-field-type='per-fess']"
             :popover-element "[data-tour='field-type-select']"
             :side "left"
             :reached {:path (conj [:ui :submenu-open?] (conj field-path :type))
                       :value true}}]
    :complete-when {:path (conj field-path :type)
                    :value :heraldry.field.type/per-fess}
    :on-complete [[::submenu/close (conj field-path :type)]]}

   {:title :string.tutorial.arms/select-subfield-title
    :description :string.tutorial.arms/select-subfield-description
    :hints [{:element (tour-node-selector subfield-I-path)
             :side "left"}]
    :complete-when {:path active-node-path
                    :pred #(= % subfield-I-path)}}

   {:title :string.tutorial.arms/set-tincture-title
    :description :string.tutorial.arms/set-tincture-description
    :hints [{:element "[data-tour='tincture-select']"
             :side "left"}
            {:element "[data-tour-tincture='or']"
             :popover-element "[data-tour='tincture-select']"
             :side "left"
             :reached {:path (conj [:ui :submenu-open?] subfield-I-tincture-path)
                       :value true}}]
    :complete-when {:path subfield-I-tincture-path
                    :value :or}
    :on-complete [[::submenu/close subfield-I-tincture-path]]}

   ;; NOTE: The subfield node delegates to field's component/node for buttons,
   ;; but the hover-menu context path is based on the subfield path, not the field path.
   {:title :string.tutorial.arms/add-charge-title
    :description :string.tutorial.arms/add-charge-description
    :hints [{:element (tour-hover-menu-selector subfield-I-path 0)
             :popover-element (tour-node-selector subfield-I-path)
             :side "left"}
            {:element (str (tour-hover-menu-selector subfield-I-path 0) " [data-tour-menu-item='charge']")
             :popover-element (tour-node-selector subfield-I-path)
             :side "left"
             :reached {:path [:ui :hover-menu-open? (conj subfield-I-path 0)]
                       :value true}}]
    :complete-when {:path subfield-I-components-path
                    :pred #(pos? (count %))}}

   {:title :string.tutorial.arms/pick-charge-title
    :description :string.tutorial.arms/pick-charge-description
    :hints [{:element "[data-tour='arms-form']"
             :side "left"}]
    :complete-when {:path (conj charge-path :type)
                    :pred #(not= % :heraldry.charge.type/roundel)}
    :on-complete [[::submenu/close (conj charge-path :type)]]}

   {:title :string.tutorial.arms/ornaments-title
    :description :string.tutorial.arms/ornaments-description
    :hints [{:element (tour-hover-menu-selector ornaments-path 0)
             :popover-element (tour-node-selector ornaments-path)
             :side "left"}
            {:element "[data-tour-menu-item='mantling']"
             :popover-element (tour-node-selector ornaments-path)
             :side "left"
             :reached {:path [:ui :hover-menu-open? (conj ornaments-path 0)]
                       :value true}}]
    :complete-when {:path ornaments-elements-path
                    :pred #(pos? (count %))}
    :on-complete [[::hover-menu/close (conj ornaments-path 0)]
                  [::submenu/close (conj ornaments-elements-path 0 :type)]]}

   {:title :string.tutorial.arms/finish-title
    :description :string.tutorial.arms/finish-description}])

(tutorial/register-tour! :arms {:goals goals})

(def ^:private entity-type :heraldicon.entity.type/arms)
(def ^:private tree-identifier :heraldicon.frontend.library.arms.details/identifier)

(rf/reg-event-fx ::start
  (fn [{:keys [db]} _]
    (reife/push-state :route.arms.details/create)
    {:db (-> db
             (update :forms dissoc entity-type)
             (tree/clear tree-identifier))
     :dispatch [::tutorial/start :arms]}))
