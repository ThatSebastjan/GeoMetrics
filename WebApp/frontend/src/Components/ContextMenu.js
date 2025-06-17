import styles from "../styles";
import Icons from "./Icons";


const ContextMenu = ({ x, y, onClose, items, featureRef }) => {

    const c = styles.ctxMenu;

    return (
    <>
        <div style={{ 
            zIndex: 1000,
            position: "absolute",
            top: "0px",
            left: "0px",
            width: "100%",
            height: "100%"}}
            onClick={() => onClose()}
            onContextMenu={(ev) => ev.preventDefault()}>
        </div>

        <c.ContextMenu $left={x} $top={y}>
            <c.Header>
                <div>Actions</div>
                <c.Close onClick={() => onClose()}>
                    <Icons.CancelIcon></Icons.CancelIcon>
                </c.Close>
            </c.Header>

            {
                items.map((itm, idx) => (
                    <c.Item key={idx}>
                        <c.Button onClick={() => {
                            itm.onClick(featureRef.current);
                            onClose();
                        }}>{itm.text}</c.Button>
                    </c.Item>)
                )
            }
        </c.ContextMenu>
    </>);

};


export default ContextMenu;