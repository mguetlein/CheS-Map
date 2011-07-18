package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import main.Settings;

public class ComponentFactory
{
	public static JLabel createLabel()
	{
		return createLabel("");
	}

	public static Border createThinBorder()
	{
		return new MatteBorder(1, 1, 1, 1, Settings.FOREGROUND);
	}

	public static Border createLineBorder(int thickness)
	{
		return new LineBorder(Settings.FOREGROUND, thickness);
	}

	public static JLabel createLabel(String text)
	{
		JLabel l = new JLabel(text);
		l.setForeground(Settings.FOREGROUND);
		return l;
	}

	public static LinkButton createLinkButton(String text)
	{
		LinkButton l = new LinkButton(text);
		l.setForegroundColor(Settings.FOREGROUND);
		l.setSelectedForegroundColor(Settings.LIST_SELECTION_FOREGROUND);
		l.setSelectedForegroundFont(l.getFont());
		return l;
	}

	public static JCheckBox createCheckBox(String text)
	{
		JCheckBox c = new JCheckBox(text);
		c.setForeground(Settings.FOREGROUND);
		return c;
	}

	public static JRadioButton createRadioButton(String text)
	{
		JRadioButton r = new JRadioButton(text);
		r.setForeground(Settings.FOREGROUND);
		return r;
	}

	static class StyleButton extends JRadioButton
	{
		public String style;

		public StyleButton(String text, boolean selected, String style)
		{
			super(text, selected);
			this.style = style;
			setForeground(Settings.FOREGROUND);
		}
	}

	public static JComboBox createComboBox()
	{
		return createComboBox(new Object[0]);
	}

	public static JComboBox createComboBox(Object[] items)
	{
		JComboBox c = new JComboBox();
		for (Object object : items)
			c.addItem(object);
		c.setForeground(Settings.FOREGROUND);
		c.setBackground(Settings.BACKGROUND);
		c.setOpaque(false);
		final Font f = new JLabel().getFont();
		c.setFont(f);
		DescriptionListCellRenderer r = new DescriptionListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				list.setSelectionBackground(Settings.LIST_ACTIVE_BACKGROUND);
				list.setSelectionForeground(Settings.LIST_SELECTION_FOREGROUND);
				list.setForeground(Settings.FOREGROUND);
				list.setBackground(Settings.BACKGROUND);
				list.setFont(f);
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		};
		r.setDescriptionForeground(Settings.FOREGROUND.darker().darker());
		c.setRenderer(r);
		return c;
	}

	public static JTable createTable()
	{
		DefaultTableModel m = new DefaultTableModel()
		{
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		JTable t = new JTable(m);
		t.getTableHeader().setVisible(false);
		t.getTableHeader().setPreferredSize(new Dimension(-1, 0));
		t.setGridColor(new Color(0, 0, 0, 0));
		t.setOpaque(false);
		t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
		{

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column)
			{
				setBackground(Settings.LIST_ACTIVE_BACKGROUND);
				if (row == 0 || isSelected)
				{
					setOpaque(true);
					setForeground(Settings.LIST_SELECTION_FOREGROUND);
				}
				else
				{
					setOpaque(false);
					setForeground(Settings.FOREGROUND);
				}
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		});

		return t;
	}

	public static JScrollPane createScrollpane(JComponent table)
	{
		JScrollPane p = new JScrollPane(table);
		p.setOpaque(false);
		p.getViewport().setOpaque(false);
		return p;
	}

}
