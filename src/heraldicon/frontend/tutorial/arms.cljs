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
  [{:title "Welcome to the Arms Editor"
    :description "Let's build a simple coat of arms. This tour will guide you through the basics — you do the clicking, and the highlights will show you where to go next."}

   {:title "Select the Field"
    :description "The Field is the content of the shield. Click the field node in the component tree to select it."
    :hints [{:element (tour-node-selector field-path)
             :side "left"}]
    :complete-when {:path active-node-path
                    :pred some?}}

   {:title "Divide the Field"
    :description "Open the Partition selector and choose \"Per Fess\" — this divides the field horizontally into two halves."
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

   {:title "Select the First Subfield"
    :description "The field is now divided into two halves. Click the first subfield in the component tree to select it."
    :hints [{:element (tour-node-selector subfield-I-path)
             :side "left"}]
    :complete-when {:path active-node-path
                    :pred #(= % subfield-I-path)}}

   {:title "Set the Tincture to Or"
    :description "Open the Tincture selector and choose \"Or\" (gold). Heraldic tinctures include metals (Or, Argent), colours (Gules, Azure, Sable, Vert, Purpure), and furs."
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
   {:title "Add a Charge"
    :description "Now let's place a charge on the field, these are figures like lions, eagles, crosses and more. Click the \"+\" button next to the subfield node, then choose \"Charge\". You'll also see Ordinary, Charge Group, and Semy here — we'll skip those for now, you can explore them later."
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

   {:title "Pick a Charge"
    :description "A roundel was added by default. Pick any charge you like — browse the tree or search by name."
    :hints [{:element "[data-tour='arms-form']"
             :side "left"}]
    :complete-when {:path (conj charge-path :type)
                    :pred #(not= % :heraldry.charge.type/roundel)}
    :on-complete [[::submenu/close (conj charge-path :type)]]}

   {:title "Ornaments"
    :description "The Ornaments section is for elements outside the shield — mantling, supporters, compartments, mottos, and more. Click the \"+\" next to Ornaments, then choose \"Mantling\"."
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

   {:title "That's the Basics!"
    :description "You've divided a field, set a tincture, added a charge, and placed mantling outside the shield. There's much more to explore — different partitions, line styles, ordinaries, the charge library, helms, and the blazon editor. Have fun!"}])

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
